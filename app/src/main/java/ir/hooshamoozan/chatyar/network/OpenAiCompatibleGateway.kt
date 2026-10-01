package ir.hooshamoozan.chatyar.network

import ir.hooshamoozan.chatyar.data.ProviderEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.Request

class OpenAiCompatibleGateway : AiGateway {
    override fun streamChat(
        provider: ProviderEntity,
        apiKey: String,
        systemPrompt: String,
        messages: List<ChatTurn>
    ): Flow<String> = flow {
        val payload = buildJsonObject {
            put("model", provider.model)
            put("stream", provider.stream)
            put("temperature", provider.temperature)
            put("max_tokens", provider.maxTokens)
            put("messages", buildJsonArray {
                if (systemPrompt.isNotBlank()) {
                    add(buildJsonObject {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                }
                messages.forEach { turn ->
                    add(buildJsonObject {
                        put("role", turn.role)
                        put("content", turn.content)
                    })
                }
            })
        }

        val url = joinUrl(provider.baseUrl, provider.endpointPath)
        val builder = postJsonRequest(url, payload.toString())
        if (apiKey.isNotBlank()) {
            builder.header(provider.authHeader, provider.authPrefix + apiKey)
        }
        applyCustomHeaders(builder, provider)

        val call = clientFor(provider).newCall(builder.build())
        val cancelHandle = currentCoroutineContext().job.invokeOnCompletion { cause ->
            if (cause is CancellationException) call.cancel()
        }
        try {
            call.execute().use { response ->
            val body = response.body ?: throw ApiException(response.code, "Empty response")
            if (!response.isSuccessful) {
                val errorText = body.string()
                throw ApiException(response.code, "HTTP ${response.code}", errorText.take(2000))
            }

            if (!provider.stream) {
                val root = JSON.parseToJsonElement(body.string()).jsonObject
                val content = root["choices"]?.jsonArray
                    ?.firstOrNull()?.jsonObject
                    ?.get("message")?.jsonObject
                    ?.get("content")?.jsonPrimitive?.content
                    .orEmpty()
                if (content.isNotEmpty()) emit(content)
                return@use
            }

            body.source().use { source ->
                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: break
                    if (!line.startsWith("data:")) continue
                    val data = line.removePrefix("data:").trim()
                    if (data.isBlank() || data == "[DONE]") continue
                    val root = runCatching { JSON.parseToJsonElement(data).jsonObject }.getOrNull() ?: continue
                    val delta = root["choices"]?.jsonArray
                        ?.firstOrNull()?.jsonObject
                        ?.get("delta")?.jsonObject
                        ?.get("content")
                    val text = runCatching { delta?.jsonPrimitive?.content }.getOrNull().orEmpty()
                    if (text.isNotEmpty()) emit(text)
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
