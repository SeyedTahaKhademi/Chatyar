package ir.hooshamoozan.chatyar.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ir.hooshamoozan.chatyar.AppContainer
import ir.hooshamoozan.chatyar.data.AppLanguage
import ir.hooshamoozan.chatyar.data.AppSettings
import ir.hooshamoozan.chatyar.data.ChatEntity
import ir.hooshamoozan.chatyar.data.MessageEntity
import ir.hooshamoozan.chatyar.data.ProviderDraft
import ir.hooshamoozan.chatyar.data.ProviderEntity
import ir.hooshamoozan.chatyar.data.ThemeMode
import ir.hooshamoozan.chatyar.network.ApiException
import ir.hooshamoozan.chatyar.network.ChatTurn
import ir.hooshamoozan.chatyar.network.ProviderProtocol
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.IOException

class MainViewModel(private val container: AppContainer) : ViewModel() {
    val settings: StateFlow<AppSettings> = container.settingsRepository.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        AppSettings()
    )

    val providers: StateFlow<List<ProviderEntity>> = container.providerRepository.providers.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val chats: StateFlow<List<ChatEntity>> = container.chatRepository.chats.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    private val generationJobs = mutableMapOf<String, Job>()
    private val _generatingChats = MutableStateFlow<Set<String>>(emptySet())
    val generatingChats: StateFlow<Set<String>> = _generatingChats

    fun provider(id: String): Flow<ProviderEntity?> = container.providerRepository.provider(id)
    fun chat(id: String): Flow<ChatEntity?> = container.chatRepository.chat(id)
    fun messages(chatId: String): Flow<List<MessageEntity>> = container.chatRepository.messages(chatId)

    fun completeOnboarding() = viewModelScope.launch {
        container.settingsRepository.setOnboarded(true)
    }

    fun setTheme(mode: ThemeMode) = viewModelScope.launch {
        container.settingsRepository.setTheme(mode)
    }

    fun setLanguage(language: AppLanguage) = viewModelScope.launch {
        container.settingsRepository.setLanguage(language)
    }

    fun loadProviderDraft(id: String, onLoaded: (ProviderDraft?) -> Unit) {
        viewModelScope.launch {
            val p = container.providerRepository.get(id)
            if (p == null) {
                onLoaded(null)
            } else {
                onLoaded(
                    ProviderDraft(
                        id = p.id,
                        name = p.name,
                        protocol = runCatching { ProviderProtocol.valueOf(p.protocol) }
                            .getOrDefault(ProviderProtocol.OPENAI_COMPATIBLE),
                        baseUrl = p.baseUrl,
                        apiKey = container.providerRepository.apiKey(p.id),
                        model = p.model,
                        endpointPath = p.endpointPath,
                        authHeader = p.authHeader,
                        authPrefix = p.authPrefix,
                        extraHeadersJson = p.extraHeadersJson,
                        temperature = p.temperature,
                        maxTokens = p.maxTokens,
                        timeoutSeconds = p.timeoutSeconds,
                        stream = p.stream
                    )
                )
            }
        }
    }

    fun saveProvider(draft: ProviderDraft, onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            val validation = validateProviderDraft(draft)
            if (validation != null) {
                onResult(Result.failure(IllegalArgumentException(validation)))
                return@launch
            }
            runCatching { container.providerRepository.save(draft) }
                .also(onResult)
        }
    }

    fun testProvider(draft: ProviderDraft, onResult: (Result<List<String>>) -> Unit) {
        viewModelScope.launch {
            if (draft.baseUrl.isBlank()) {
                onResult(Result.failure(IllegalArgumentException("Base URL is required")))
                return@launch
            }
            if (!isValidHeadersJson(draft.extraHeadersJson)) {
                onResult(Result.failure(IllegalArgumentException("Invalid headers JSON")))
                return@launch
            }
            val now = System.currentTimeMillis()
            val temp = ProviderEntity(
                id = draft.id ?: "test",
                name = draft.name,
                protocol = draft.protocol.name,
                baseUrl = draft.baseUrl.trim().trimEnd('/'),
                model = draft.model.trim(),
                endpointPath = draft.endpointPath.ifBlank { "/chat/completions" },
                authHeader = draft.authHeader.ifBlank { "Authorization" },
                authPrefix = draft.authPrefix,
                extraHeadersJson = draft.extraHeadersJson.ifBlank { "{}" },
                temperature = draft.temperature,
                maxTokens = draft.maxTokens,
                timeoutSeconds = draft.timeoutSeconds,
                stream = draft.stream,
                createdAt = now,
                updatedAt = now
            )
            val gateway = container.gatewayFactory.create(draft.protocol)
            runCatching { gateway.listModels(temp, draft.apiKey) }
                .recoverCatching { throw FriendlyApiException(friendlyError(it)) }
                .also(onResult)
        }
    }

    fun deleteProvider(provider: ProviderEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            container.providerRepository.delete(provider)
            onDone()
        }
    }

    fun createChat(providerId: String, systemPrompt: String, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val id = container.chatRepository.createChat(providerId, systemPrompt.trim())
            onCreated(id)
        }
    }

    fun deleteChat(chat: ChatEntity) {
        stopGeneration(chat.id)
        viewModelScope.launch { container.chatRepository.delete(chat) }
    }

    fun sendMessage(chatId: String, text: String) {
        val clean = text.trim()
        if (clean.isEmpty() || generationJobs[chatId]?.isActive == true) return

        val job = viewModelScope.launch {
            _generatingChats.value = _generatingChats.value + chatId
            var assistantId: String? = null
            try {
                val chat = container.chatRepository.getChat(chatId)
                    ?: throw IllegalStateException("Chat not found")
                val provider = container.providerRepository.get(chat.providerId)
                    ?: throw IllegalStateException("Provider not found")
                val key = container.providerRepository.apiKey(provider.id)

                container.chatRepository.addMessage(chatId, "user", clean)
                val history = container.chatRepository.getMessages(chatId)
                    .filter { !it.isError }
                    .map { ChatTurn(it.role, it.content) }
                assistantId = container.chatRepository.addMessage(chatId, "assistant", "")

                val protocol = runCatching { ProviderProtocol.valueOf(provider.protocol) }
                    .getOrDefault(ProviderProtocol.OPENAI_COMPATIBLE)
                val gateway = container.gatewayFactory.create(protocol)
                val buffer = StringBuilder()

                gateway.streamChat(provider, key, chat.systemPrompt, history).collect { delta ->
                    buffer.append(delta)
                    container.chatRepository.updateMessage(assistantId, buffer.toString(), false)
                }

                if (buffer.isEmpty()) {
                    container.chatRepository.updateMessage(
                        assistantId,
                        friendlyError(IllegalStateException("Empty model response")),
                        true
                    )
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (t: Throwable) {
                val msg = friendlyError(t)
                if (assistantId == null) {
                    container.chatRepository.addMessage(chatId, "assistant", msg, true)
                } else {
                    container.chatRepository.updateMessage(assistantId, msg, true)
                }
            } finally {
                _generatingChats.value = _generatingChats.value - chatId
                generationJobs.remove(chatId)
            }
        }
        generationJobs[chatId] = job
    }

    fun stopGeneration(chatId: String) {
        generationJobs.remove(chatId)?.cancel()
        _generatingChats.value = _generatingChats.value - chatId
    }

    private fun validateProviderDraft(draft: ProviderDraft): String? {
        if (draft.name.isBlank() || draft.baseUrl.isBlank() || draft.model.isBlank()) {
            return "Required fields are missing"
        }
        if (!draft.baseUrl.startsWith("https://") && !draft.baseUrl.startsWith("http://")) {
            return "Base URL must start with http:// or https://"
        }
        if (!isValidHeadersJson(draft.extraHeadersJson)) return "Invalid headers JSON"
        return null
    }

    private fun isValidHeadersJson(value: String): Boolean {
        if (value.isBlank()) return true
        return runCatching { Json.parseToJsonElement(value) }.getOrNull() is kotlinx.serialization.json.JsonObject
    }

    private fun friendlyError(t: Throwable): String {
        if (t is FriendlyApiException) return t.message.orEmpty()
        val s = textsFor(settings.value.language)
        if (t is ApiException) {
            val code = t.statusCode
            return when {
                code == 401 -> s.errorUnauthorized
                code == 403 -> s.errorForbidden
                code == 404 -> s.errorNotFound
                code == 429 -> s.errorRateLimit
                code != null && code in 500..599 -> s.errorServer
                else -> buildString {
                    append(s.errorGeneric)
                    if (code != null) append(" HTTP $code.")
                    val body = t.responseBody?.trim()?.take(700)
                    if (!body.isNullOrBlank()) append("\n\n$body")
                }
            }
        }
        return when (t) {
            is IOException -> s.errorNetwork
            is IllegalArgumentException -> t.message ?: s.errorGeneric
            else -> t.message?.takeIf { it.isNotBlank() } ?: s.errorGeneric
        }
    }
}

private class FriendlyApiException(message: String) : Exception(message)

class MainViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(container) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
