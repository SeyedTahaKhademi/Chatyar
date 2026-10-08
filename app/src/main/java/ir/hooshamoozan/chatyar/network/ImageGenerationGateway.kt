package ir.hooshamoozan.chatyar.network

import android.util.Base64
import ir.hooshamoozan.chatyar.data.ProviderEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.*
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.IOException

/**
 * The OpenAI-compatible /images/generations protocol. Providers supporting other
 * image APIs (e.g. Gemini native image content, Stability, Replicate) require a
 * separate gateway; they must not be incorrectly routed through this endpoint.
 */
class ImageGenerationGateway {
    suspend fun generate(
        provider: ProviderEntity,
        apiKey: String,
        prompt: String,
        model: String,
        size: String,
        endpoint: String = "/images/generations"
    ): ByteArray = withContext(Dispatchers.IO) {
        require(prompt.isNotBlank()) { "Image prompt is required" }
        require(model.isNotBlank()) { "Enter the image model ID" }
        require(size in setOf("1024x1024", "1024x1536", "1536x1024", "512x512")) { "Unsupported image size" }
        require(endpoint.startsWith("/") && !endpoint.startsWith("//") && "://" !in endpoint) { "Invalid image endpoint" }
        val body = buildJsonObject {
            put("model", model.trim())
            put("prompt", prompt.trim())
            put("n", 1)
            put("size", size)
        }
        val builder = postJsonRequest(joinUrl(provider.baseUrl, endpoint), body.toString())
        if (apiKey.isNotBlank()) builder.header(provider.authHeader, provider.authPrefix + apiKey)
        applyCustomHeaders(builder, provider)
        val call = clientFor(provider).newCall(builder.build())
        val registration = currentCoroutineContext().job.invokeOnCompletion { if (it is CancellationException) call.cancel() }
        try {
            val reply = call.execute().use { response ->
                val payload = response.body?.string().orEmpty()
                requireSuccess(response.code, response.message, payload)
                JSON.parseToJsonElement(payload).jsonObject
            }
            val image = reply["data"]?.jsonArray?.firstOrNull()?.jsonObject
                ?: throw IOException("No image in provider response. This provider may use a different image API protocol.")
            val base64Image = image["b64_json"]?.jsonPrimitive?.contentOrNull
            if (!base64Image.isNullOrBlank()) {
                Base64.decode(base64Image, Base64.DEFAULT)
            } else {
                val url = image["url"]?.jsonPrimitive?.contentOrNull
                    ?: throw IOException("Provider returned no b64_json or image URL")
                val uri = url.toHttpUrlOrNull()
                    ?: throw IOException("Invalid image URL")
                // No API key is forwarded to this address (possibly a third-party CDN).
                if (!uri.isHttps) throw IOException("Insecure image download URL is not accepted")
                val imageCall = clientFor(provider).newCall(Request.Builder().url(uri).get().build())
                val imageCancellation = currentCoroutineContext().job.invokeOnCompletion {
                    if (it is CancellationException) imageCall.cancel()
                }
                try {
                    imageCall.execute().use { response ->
                        if (!response.isSuccessful) throw IOException("Image download failed: HTTP ${response.code}")
                        val responseBody = response.body ?: throw IOException("Empty image download")
                        // Avoid unbounded memory usage from a provider-controlled response.
                        val length = responseBody.contentLength()
                        if (length > 20_000_000L) throw IOException("Image is too large")
                        responseBody.byteStream().use { stream ->
                            val result = java.io.ByteArrayOutputStream()
                            val buffer = ByteArray(8192)
                            while (true) {
                                val count = stream.read(buffer)
                                if (count == -1) break
                                result.write(buffer, 0, count)
                                if (result.size() > 20_000_000) throw IOException("Image is too large")
                            }
                            result.toByteArray()
                        }
                    }
                } finally { imageCancellation.dispose() }
            }
        } finally { registration.dispose() }
    }
}
