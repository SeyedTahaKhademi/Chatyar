package ir.hooshamoozan.chatyar.network

import ir.hooshamoozan.chatyar.data.ProviderEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import okhttp3.Request

class OpenAiResponsesGateway : AiGateway {
    override fun streamChat(provider: ProviderEntity, apiKey: String, systemPrompt: String, messages: List<ChatTurn>): Flow<String> = flow {
        val payload = buildJsonObject {
            put("model", provider.model); put("stream", provider.stream); put("max_output_tokens", provider.maxTokens)
            if (systemPrompt.isNotBlank()) put("instructions", systemPrompt)
            put("input", buildJsonArray { messages.forEach { turn -> add(buildJsonObject { put("type", "message"); put("role", if (turn.role == "assistant") "assistant" else "user"); put("content", turn.content) }) } })
        }
        val builder = postJsonRequest(joinUrl(provider.baseUrl, provider.endpointPath.ifBlank { "/responses" }), payload.toString())
        if (apiKey.isNotBlank()) builder.header(provider.authHeader, provider.authPrefix + apiKey)
        if (provider.stream) builder.header("Accept", "text/event-stream")
        applyCustomHeaders(builder, provider)
        val call = clientFor(provider).newCall(builder.build())
        val cancelHandle = currentCoroutineContext().job.invokeOnCompletion { if (it is CancellationException) call.cancel() }
        try {
            call.execute().use { response ->
                val body = response.body ?: throw ApiException(response.code, "Empty response")
                if (!response.isSuccessful) throw ApiException(response.code, "HTTP ${response.code}", body.string().take(2000))
                if (!provider.stream) {
                    val root = JSON.parseToJsonElement(body.string()).jsonObject
                    val text = root["output"]?.jsonArray.orEmpty().joinToString("") { item ->
                        runCatching { item.jsonObject["content"]?.jsonArray.orEmpty().joinToString("") { part ->
                            val obj = part.jsonObject; if (obj["type"]?.jsonPrimitive?.content == "output_text") obj["text"]?.jsonPrimitive?.content.orEmpty() else ""
                        }}.getOrDefault("")
                    }
                    if (text.isNotEmpty()) emit(text); return@use
                }
                body.source().use { source -> while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: break; if (!line.startsWith("data:")) continue
                    val data = line.removePrefix("data:").trim(); if (data.isBlank() || data == "[DONE]") continue
                    val root = runCatching { JSON.parseToJsonElement(data).jsonObject }.getOrNull() ?: continue
                    when (root["type"]?.jsonPrimitive?.content) {
                        "response.output_text.delta", "response.refusal.delta" -> root["delta"]?.jsonPrimitive?.content.orEmpty().takeIf { it.isNotEmpty() }?.let { emit(it) }
                        "error" -> throw ApiException(response.code, root["message"]?.jsonPrimitive?.content ?: "OpenAI stream error", data.take(2000))
                    }
                }}
            }
        } finally { cancelHandle.dispose() }
    }.flowOn(Dispatchers.IO)

    override suspend fun listModels(provider: ProviderEntity, apiKey: String): List<String> = withContext(Dispatchers.IO) {
        val builder = commonHeaders(Request.Builder().url(joinUrl(provider.baseUrl, "/models")).get())
        if (apiKey.isNotBlank()) builder.header(provider.authHeader, provider.authPrefix + apiKey)
        applyCustomHeaders(builder, provider)
        clientFor(provider).newCall(builder.build()).execute().use { response ->
            val text = response.body?.string().orEmpty(); requireSuccess(response.code, response.message, text); extractModelIds(text)
        }
    }
}
