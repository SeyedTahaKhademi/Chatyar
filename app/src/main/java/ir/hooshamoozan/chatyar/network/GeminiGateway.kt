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

class GeminiGateway : AiGateway {
    override fun streamChat(
        provider: ProviderEntity,
        apiKey: String,
        systemPrompt: String,
        messages: List<ChatTurn>
    ): Flow<String> = flow {
        val payload = buildJsonObject {
            if (systemPrompt.isNotBlank()) {
                put("systemInstruction", buildJsonObject {
                    put("parts", buildJsonArray {
                        add(buildJsonObject { put("text", systemPrompt) })
                    })
                })
            }
            put("contents", buildJsonArray {
                messages.forEach { turn ->
                    add(buildJsonObject {
                        put("role", if (turn.role == "assistant") "model" else "user")
                        put("parts", buildJsonArray {
                            add(buildJsonObject { put("text", turn.content) })
                        })
                    })
                }
            })
            put("generationConfig", buildJsonObject {
                put("temperature", provider.temperature)
                put("maxOutputTokens", provider.maxTokens)
            })
        }

        val endpoint = if (provider.stream) {
            "/v1beta/models/${provider.model}:streamGenerateContent?alt=sse"
        } else {
            "/v1beta/models/${provider.model}:generateContent"
        }
        val builder = postJsonRequest(joinUrl(provider.baseUrl, endpoint), payload.toString())
            .header("x-goog-api-key", apiKey)
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
                val text = extractGeminiText(root)
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
                    val text = extractGeminiText(root)
                    if (text.isNotEmpty()) emit(text)
                }
            }
            }
        } finally {
            cancelHandle.dispose()
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun listModels(provider: ProviderEntity, apiKey: String): List<String> = withContext(Dispatchers.IO) {
        val builder = Request.Builder()
            .url(joinUrl(provider.baseUrl, "/v1beta/models"))
            .get()
            .header("x-goog-api-key", apiKey)
        applyCustomHeaders(builder, provider)
        clientFor(provider).newCall(builder.build()).execute().use { response ->
            val text = response.body?.string().orEmpty()
            requireSuccess(response.code, response.message, text)
            val root = JSON.parseToJsonElement(text).jsonObject
            root["models"]?.jsonArray.orEmpty().mapNotNull { item ->
                runCatching { item.jsonObject["name"]?.jsonPrimitive?.content?.removePrefix("models/") }.getOrNull()
            }.sorted()
        }
    }

    private fun extractGeminiText(root: kotlinx.serialization.json.JsonObject): String =
        root["candidates"]?.jsonArray
            ?.firstOrNull()?.jsonObject
            ?.get("content")?.jsonObject
            ?.get("parts")?.jsonArray.orEmpty()
            .joinToString("") { part ->
                runCatching { part.jsonObject["text"]?.jsonPrimitive?.content.orEmpty() }.getOrDefault("")
            }
}
