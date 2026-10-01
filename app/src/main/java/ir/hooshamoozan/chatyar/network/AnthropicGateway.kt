package ir.hooshamoozan.chatyar.network

import ir.hooshamoozan.chatyar.data.ProviderEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.Request

class AnthropicGateway : AiGateway {
    override fun streamChat(
        provider: ProviderEntity,
        apiKey: String,
        systemPrompt: String,
        messages: List<ChatTurn>
    ): Flow<String> = flow {
        val payload = buildJsonObject {
            put("model", provider.model)
            put("max_tokens", provider.maxTokens)
            put("temperature", provider.temperature.coerceIn(0.0, 1.0))
            put("stream", provider.stream)
            if (systemPrompt.isNotBlank()) put("system", systemPrompt)
            put("messages", buildJsonArray {
                messages.forEach { turn ->
                    add(buildJsonObject {
                        put("role", if (turn.role == "assistant") "assistant" else "user")
                        put("content", turn.content)
                    })
                }
            })
        }
        val builder = postJsonRequest(joinUrl(provider.baseUrl, "/v1/messages"), payload.toString())
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
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
                val text = root["content"]?.jsonArray.orEmpty().joinToString("") { block ->
                    runCatching {
                        val obj = block.jsonObject
                        if (obj["type"]?.jsonPrimitive?.content == "text") obj["text"]?.jsonPrimitive?.content.orEmpty() else ""
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
                    if (data.isBlank()) continue
                    val root = runCatching { JSON.parseToJsonElement(data).jsonObject }.getOrNull() ?: continue
                    if (root["type"]?.jsonPrimitive?.content != "content_block_delta") continue
                    val delta = root["delta"]?.jsonObject ?: continue
                    if (delta["type"]?.jsonPrimitive?.content == "text_delta") {
                        val text = delta["text"]?.jsonPrimitive?.content.orEmpty()
                        if (text.isNotEmpty()) emit(text)
                    }
                }
            }
            }
        } finally {
            cancelHandle.dispose()
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun listModels(provider: ProviderEntity, apiKey: String): List<String> = withContext(Dispatchers.IO) {
        val builder = Request.Builder()
            .url(joinUrl(provider.baseUrl, "/v1/models"))
            .get()
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
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
