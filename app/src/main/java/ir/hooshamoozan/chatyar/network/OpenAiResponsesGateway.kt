package ir.hooshamoozan.chatyar.network

import ir.hooshamoozan.chatyar.data.ProviderEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.Request

/**
 * Native OpenAI Responses API adapter. Kept separate from the generic
 * Chat Completions adapter so current OpenAI models can use /v1/responses
 * while third-party OpenAI-compatible providers keep /chat/completions.
 */
class OpenAiResponsesGateway : AiGateway {
    override fun streamChat(
        provider: ProviderEntity,
        apiKey: String,
        systemPrompt: String,
        messages: List<ChatTurn>
    ): Flow<String> = flow {
        val payload = buildJsonObject {
            put("model", provider.model)
            put("stream", provider.stream)
            put("max_output_tokens", provider.maxTokens)
            if (systemPrompt.isNotBlank()) put("instructions", systemPrompt)
            put("input", buildJsonArray {
                messages.forEach { turn ->
                    add(buildJsonObject {
                        put("type", "message")
                        put("role", if (turn.role == "assistant") "assistant" else "user")
                        put("content", turn.content)
                    })
                }
            })
        }

        val path = provider.endpointPath.ifBlank { "/responses" }
        val builder = postJsonRequest(joinUrl(provider.baseUrl, path), payload.toString())
        if (apiKey.isNotBlank()) builder.header(provider.authHeader, provider.authPrefix + apiKey)
        applyCustomHeaders(builder, provider)

        val call = clientFor(provider).newCall(builder.build())
        val cancelHandle = currentCoroutineContext().job.invokeOnCompletion { cause ->
            if (cause is CancellationException) call.cancel()
        }
        try {
            call.execute().use { response ->
                val body = response.body ?: throw ApiException(response.code, "Empty response")
                if (!response.isSuccessful) {
                    throw ApiException(response.code, "HTTP ${response.code}", body.string().take(2000))
                }

                if (!provider.stream) {
                    val root = JSON.parseToJsonElement(body.string()).jsonObject
                    val text = root["output"]?.jsonArray.orEmpty().joinToString("") { item ->
                        runCatching {
                            item.jsonObject["content"]?.jsonArray.orEmpty().joinToString("") { part ->
                                val obj = part.jsonObject
                                if (obj["type"]?.jsonPrimitive?.content == "output_text") {
                                    obj["text"]?.jsonPrimitive?.content.orEmpty()
                                } else ""
                            }
                        }.getOrDefault("")
                    }
                    if (text.isNotEmpty()) emit(text)
                    return@use
                }

                body.source().use { source ->
                    while (!source.exhausted()) {
                        val line = source.readUtf8Line() ?: break
                        if (!line.startsWith("data:")) continue
                        val data = line.removePrefix("data:").trim()
                        if (data.isBlank() || data == "[DONE]") continue
                        val root = runCatching { JSON.parseToJsonElement(data).jsonObject }.getOrNull() ?: continue
                        when (root["type"]?.jsonPrimitive?.content) {
                            "response.output_text.delta", "response.refusal.delta" -> {
                                val delta = root["delta"]?.jsonPrimitive?.content.orEmpty()
                                if (delta.isNotEmpty()) emit(delta)
                            }
                            "error" -> {
                                val message = root["message"]?.jsonPrimitive?.content ?: "OpenAI stream error"
                                throw ApiException(response.code, message, data.take(2000))
                            }
                        }
                    }
                }
            }
        } finally {
            cancelHandle.dispose()
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun listModels(provider: ProviderEntity, apiKey: String): List<String> = withContext(Dispatchers.IO) {
        val builder = Request.Builder().url(joinUrl(provider.baseUrl, "/models")).get()
        if (apiKey.isNotBlank()) builder.header(provider.authHeader, provider.authPrefix + apiKey)
        applyCustomHeaders(builder, provider)
        clientFor(provider).newCall(builder.build()).execute().use { response ->
            val text = response.body?.string().orEmpty()
            requireSuccess(response.code, response.message, text)
            val root = JSON.parseToJsonElement(text).jsonObject
            root["data"]?.jsonArray.orEmpty().mapNotNull { item ->
                runCatching { item.jsonObject["id"]?.jsonPrimitive?.content }.getOrNull()
            }.sorted()
        }
    }
}
