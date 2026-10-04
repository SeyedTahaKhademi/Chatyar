package ir.hooshamoozan.chatyar.network

import ir.hooshamoozan.chatyar.data.ProviderEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

internal val JSON = Json { ignoreUnknownKeys = true; isLenient = true }
internal val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
data class ChatTurn(val role: String, val content: String)

class ApiException(val statusCode: Int? = null, message: String, val responseBody: String? = null) : IOException(message)

interface AiGateway {
    fun streamChat(provider: ProviderEntity, apiKey: String, systemPrompt: String, messages: List<ChatTurn>): Flow<String>
    suspend fun listModels(provider: ProviderEntity, apiKey: String): List<String>
}

class AiGatewayFactory {
    fun create(protocol: ProviderProtocol): AiGateway = when (protocol) {
        ProviderProtocol.OPENAI_RESPONSES -> OpenAiResponsesGateway()
        ProviderProtocol.OPENAI_COMPATIBLE -> OpenAiCompatibleGateway()
        ProviderProtocol.ANTHROPIC -> AnthropicGateway()
        ProviderProtocol.GEMINI -> GeminiGateway()
    }
}

internal fun clientFor(provider: ProviderEntity): OkHttpClient {
    val seconds = provider.timeoutSeconds.coerceIn(10, 180).toLong()
    return OkHttpClient.Builder()
        .connectTimeout(minOf(seconds, 20L), TimeUnit.SECONDS)
        .readTimeout(seconds, TimeUnit.SECONDS)
        .writeTimeout(minOf(seconds, 30L), TimeUnit.SECONDS)
        .callTimeout(seconds, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
}

internal fun joinUrl(base: String, path: String): String = base.trimEnd('/') + "/" + path.trimStart('/')

internal fun commonHeaders(builder: Request.Builder): Request.Builder = builder
    .header("Accept", "application/json")
    .header("User-Agent", "Chatyar-Android/0.2")

internal fun applyCustomHeaders(builder: Request.Builder, provider: ProviderEntity) {
    val text = provider.extraHeadersJson.trim()
    if (text.isBlank() || text == "{}") return
    runCatching {
        JSON.parseToJsonElement(text).jsonObject.forEach { (k, v) ->
            val lower = k.lowercase()
            if (lower !in setOf("authorization", "content-type", "x-api-key", "anthropic-version", "x-goog-api-key")) {
                builder.header(k, v.jsonPrimitive.content)
            }
        }
    }
}

internal fun requireSuccess(responseCode: Int, responseMessage: String, body: String) {
    if (responseCode !in 200..299) throw ApiException(responseCode, "HTTP $responseCode $responseMessage", body.take(2000))
}

internal fun postJsonRequest(url: String, jsonBody: String): Request.Builder = commonHeaders(
    Request.Builder().url(url).post(jsonBody.toRequestBody(JSON_MEDIA)).header("Content-Type", "application/json")
)

internal fun extractModelIds(body: String): List<String> {
    val root = runCatching { JSON.parseToJsonElement(body) }.getOrNull() ?: return emptyList()
    fun readItem(item: JsonElement): String? = when (item) {
        is JsonPrimitive -> item.contentOrNull
        is JsonObject -> sequenceOf("id", "name", "model", "slug")
            .mapNotNull { key -> item[key]?.jsonPrimitive?.contentOrNull }
            .firstOrNull()
        else -> null
    }
    val items: List<JsonElement> = when (root) {
        is JsonArray -> root
        is JsonObject -> {
            val candidate = sequenceOf("data", "models", "result", "items")
                .mapNotNull { root[it] }
                .firstOrNull { it is JsonArray }
            (candidate as? JsonArray)?.toList().orEmpty()
        }
        else -> emptyList()
    }
    return items.mapNotNull(::readItem).map { it.removePrefix("models/") }.distinct().sorted()
}
