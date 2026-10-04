package ir.hooshamoozan.chatyar.ui

import androidx.lifecycle.*
import ir.hooshamoozan.chatyar.AppContainer
import ir.hooshamoozan.chatyar.data.*
import ir.hooshamoozan.chatyar.network.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.URI
import java.util.ArrayDeque

class MainViewModel(private val container: AppContainer) : ViewModel() {
    companion object {
        private const val MAX_CONTEXT_MESSAGES = 40
        private const val MAX_CONTEXT_CHARS = 60_000
        private const val TEST_TIMEOUT_MS = 15_000L
        private const val TEST_HTTP_TIMEOUT_SECONDS = 12
    }

    val settings = container.settingsRepository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())
    val providers = container.providerRepository.providers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val chats = container.chatRepository.chats.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val generationJobs = mutableMapOf<String, Job>()
    private val _generatingChats = MutableStateFlow<Set<String>>(emptySet())
    val generatingChats: StateFlow<Set<String>> = _generatingChats

    fun provider(id: String): Flow<ProviderEntity?> = container.providerRepository.provider(id)
    fun chat(id: String): Flow<ChatEntity?> = container.chatRepository.chat(id)
    fun messages(chatId: String): Flow<List<MessageEntity>> = container.chatRepository.messages(chatId)
    fun completeOnboarding() = viewModelScope.launch { container.settingsRepository.setOnboarded(true) }
    fun setTheme(mode: ThemeMode) = viewModelScope.launch { container.settingsRepository.setTheme(mode) }
    fun setLanguage(language: AppLanguage) = viewModelScope.launch { container.settingsRepository.setLanguage(language) }
    fun clearAllChats() = viewModelScope.launch { generationJobs.values.forEach { it.cancel() }; generationJobs.clear(); container.chatRepository.clearAllChats() }

    fun loadProviderDraft(id: String, onLoaded: (ProviderDraft?) -> Unit) = viewModelScope.launch {
        val p = container.providerRepository.get(id)
        onLoaded(p?.let {
            ProviderDraft(it.id, it.name, runCatching { ProviderProtocol.valueOf(it.protocol) }.getOrDefault(ProviderProtocol.OPENAI_COMPATIBLE), it.baseUrl,
                container.providerRepository.apiKey(it.id), it.model, it.endpointPath, it.authHeader, it.authPrefix, it.extraHeadersJson,
                it.temperature, it.maxTokens, it.timeoutSeconds, it.stream)
        })
    }

    fun saveProvider(draft: ProviderDraft, onResult: (Result<String>) -> Unit) = viewModelScope.launch {
        val validation = validateProviderDraft(draft)
        if (validation != null) { onResult(Result.failure(IllegalArgumentException(validation))); return@launch }
        runCatching { container.providerRepository.save(draft) }.also(onResult)
    }

    fun testProvider(draft: ProviderDraft, onResult: (Result<List<String>>) -> Unit) = viewModelScope.launch {
        val validation = validateProviderForTest(draft)
        if (validation != null) { onResult(Result.failure(IllegalArgumentException(validation))); return@launch }
        val testProvider = providerEntityFromDraft(draft).copy(timeoutSeconds = TEST_HTTP_TIMEOUT_SECONDS)
        val gateway = container.gatewayFactory.create(draft.protocol)

        val modelDiscovery = runCatching {
            withTimeout(TEST_TIMEOUT_MS) { gateway.listModels(testProvider, draft.apiKey.trim()) }
        }
        if (modelDiscovery.isSuccess) {
            onResult(Result.success(modelDiscovery.getOrThrow())); return@launch
        }

        if (draft.model.isBlank()) {
            val cause = modelDiscovery.exceptionOrNull()
            onResult(Result.failure(FriendlyApiException(testFailureMessage(cause)))); return@launch
        }

        val probe = testProvider.copy(stream = false, maxTokens = minOf(testProvider.maxTokens, 8))
        runCatching {
            withTimeout(TEST_TIMEOUT_MS) {
                gateway.streamChat(probe, draft.apiKey.trim(), "", listOf(ChatTurn("user", "Reply with OK."))).first()
            }
            emptyList<String>()
        }.recoverCatching { throw FriendlyApiException(testFailureMessage(it)) }.also(onResult)
    }

    fun deleteProvider(provider: ProviderEntity, onDone: () -> Unit = {}) = viewModelScope.launch { container.providerRepository.delete(provider); onDone() }
    fun createChat(providerId: String, systemPrompt: String, onCreated: (String) -> Unit) = viewModelScope.launch { onCreated(container.chatRepository.createChat(providerId, systemPrompt.trim())) }
    fun deleteChat(chat: ChatEntity) { stopGeneration(chat.id); viewModelScope.launch { container.chatRepository.delete(chat) } }

    fun sendMessage(chatId: String, text: String) {
        val clean = text.trim(); if (clean.isEmpty() || generationJobs[chatId]?.isActive == true) return
        val job = viewModelScope.launch {
            _generatingChats.value = _generatingChats.value + chatId
            var assistantId: String? = null; var assistantText = ""
            try {
                val chat = container.chatRepository.getChat(chatId) ?: error("Chat not found")
                val provider = container.providerRepository.get(chat.providerId) ?: error("Provider not found")
                val key = container.providerRepository.apiKey(provider.id)
                val protocol = runCatching { ProviderProtocol.valueOf(provider.protocol) }.getOrDefault(ProviderProtocol.OPENAI_COMPATIBLE)
                val draft = ProviderDraft(id=provider.id,name=provider.name,protocol=protocol,baseUrl=provider.baseUrl,apiKey=key,model=provider.model)
                if (requiresApiKey(draft) && key.isBlank()) throw IllegalArgumentException("API key is missing for this provider")

                container.chatRepository.addMessage(chatId, "user", clean)
                val history = buildContext(container.chatRepository.getMessages(chatId))
                val responseMessageId = container.chatRepository.addMessage(chatId, "assistant", "")
                assistantId = responseMessageId
                val gateway = container.gatewayFactory.create(protocol)
                gateway.streamChat(provider, key, chat.systemPrompt, history).collect { delta ->
                    assistantText += delta
                    container.chatRepository.updateMessage(responseMessageId, assistantText, false)
                }
                if (assistantText.isBlank()) container.chatRepository.updateMessage(responseMessageId, friendlyError(IllegalStateException("Empty model response")), true)
            } catch (cancel: CancellationException) {
                if (assistantText.isBlank()) assistantId?.let { container.chatRepository.deleteMessage(it) }
                throw cancel
            } catch (t: Throwable) {
                val msg = friendlyError(t)
                if (assistantId == null) container.chatRepository.addMessage(chatId, "assistant", msg, true)
                else assistantId?.let { container.chatRepository.updateMessage(it, msg, true) }
            } finally {
                _generatingChats.value = _generatingChats.value - chatId
                generationJobs.remove(chatId)
            }
        }
        generationJobs[chatId] = job
    }

    fun stopGeneration(chatId: String) { generationJobs.remove(chatId)?.cancel(); _generatingChats.value = _generatingChats.value - chatId }

    private fun providerEntityFromDraft(draft: ProviderDraft): ProviderEntity {
        val now = System.currentTimeMillis()
        return ProviderEntity(draft.id ?: "connection-test", draft.name, draft.protocol.name, draft.baseUrl.trim().trimEnd('/'), draft.model.trim(),
            draft.endpointPath.trim().ifBlank { "/chat/completions" }, draft.authHeader.trim().ifBlank { "Authorization" }, draft.authPrefix,
            draft.extraHeadersJson.trim().ifBlank { "{}" }, draft.temperature.coerceIn(0.0,2.0), draft.maxTokens.coerceIn(1,262144),
            draft.timeoutSeconds.coerceIn(10,180), draft.stream, now, now)
    }

    private fun buildContext(messages: List<MessageEntity>): List<ChatTurn> {
        val selected = ArrayDeque<ChatTurn>(); var chars = 0
        for (message in messages.asReversed()) {
            if (message.isError || message.content.isBlank()) continue
            if (selected.size >= MAX_CONTEXT_MESSAGES) break
            val len = message.content.length
            if (selected.isNotEmpty() && chars + len > MAX_CONTEXT_CHARS) break
            selected.addFirst(ChatTurn(message.role, message.content)); chars += len
        }
        return selected.toList()
    }

    private fun validateProviderForTest(draft: ProviderDraft): String? {
        if (draft.baseUrl.isBlank()) return "Base URL is required"
        if (!draft.baseUrl.startsWith("https://") && !draft.baseUrl.startsWith("http://")) return "Base URL must start with http:// or https://"
        if (requiresApiKey(draft) && draft.apiKey.isBlank()) return "API key is required for this provider"
        if (!isValidHeadersJson(draft.extraHeadersJson)) return "Invalid headers JSON"
        return null
    }

    private fun validateProviderDraft(draft: ProviderDraft): String? {
        if (draft.name.isBlank() || draft.baseUrl.isBlank() || draft.model.isBlank()) return "Required fields are missing"
        return validateProviderForTest(draft)
    }

    private fun requiresApiKey(draft: ProviderDraft): Boolean {
        when (draft.protocol) {
            ProviderProtocol.OPENAI_RESPONSES, ProviderProtocol.ANTHROPIC, ProviderProtocol.GEMINI -> return true
            ProviderProtocol.OPENAI_COMPATIBLE -> Unit
        }
        val host = runCatching { URI(draft.baseUrl.trim()).host?.lowercase() }.getOrNull() ?: return false
        val hosted = listOf("openrouter.ai","groq.com","mistral.ai","together.xyz","deepseek.com","x.ai","openai.com","cleanapis.com")
        return hosted.any { host == it || host.endsWith(".$it") }
    }

    private fun isValidHeadersJson(value: String): Boolean = value.isBlank() || runCatching { Json.parseToJsonElement(value) }.getOrNull() is kotlinx.serialization.json.JsonObject

    private fun testFailureMessage(t: Throwable?): String {
        val fa = settings.value.language == AppLanguage.FA
        return when (t) {
            is TimeoutCancellationException -> if (fa) "سرور در ۱۵ ثانیه پاسخ نداد. Base URL، API Key و مسیر شبکه را بررسی کن." else "The server did not respond within 15 seconds. Check Base URL, API key and network path."
            else -> friendlyError(t ?: IOException("Connection test failed"))
        }
    }

    private fun friendlyError(t: Throwable): String {
        if (t is FriendlyApiException) return t.message.orEmpty()
        val s = textsFor(settings.value.language)
        if (t is ApiException) {
            val code = t.statusCode
            return when {
                code == 400 -> s.errorGeneric + t.responseBody?.trim()?.take(700)?.let { "\n\n$it" }.orEmpty()
                code == 401 -> s.errorUnauthorized
                code == 403 -> s.errorForbidden
                code == 404 -> s.errorNotFound
                code == 408 -> s.errorNetwork
                code == 429 -> s.errorRateLimit
                code != null && code in 500..599 -> s.errorServer
                else -> s.errorGeneric + (code?.let { " HTTP $it." } ?: "") + t.responseBody?.trim()?.take(700)?.let { "\n\n$it" }.orEmpty()
            }
        }
        return when (t) { is IOException -> s.errorNetwork; is IllegalArgumentException -> t.message ?: s.errorGeneric; else -> t.message?.takeIf { it.isNotBlank() } ?: s.errorGeneric }
    }
}

private class FriendlyApiException(message: String) : Exception(message)
class MainViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) return MainViewModel(container) as T
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
