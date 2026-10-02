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
import java.net.URI
import java.util.ArrayDeque

class MainViewModel(
    private val container: AppContainer
) : ViewModel() {

    companion object {
        /*
         * Simple context protection.
         *
         * This is deliberately conservative and does not pretend to be an
         * exact tokenizer. The goal is to prevent a very long local chat from
         * being sent in full on every request.
         */
        private const val MAX_CONTEXT_MESSAGES = 40
        private const val MAX_CONTEXT_CHARS = 60_000
    }

    val settings: StateFlow<AppSettings> =
        container.settingsRepository.settings.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            AppSettings()
        )

    val providers: StateFlow<List<ProviderEntity>> =
        container.providerRepository.providers.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val chats: StateFlow<List<ChatEntity>> =
        container.chatRepository.chats.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    private val generationJobs =
        mutableMapOf<String, Job>()

    private val _generatingChats =
        MutableStateFlow<Set<String>>(emptySet())

    val generatingChats: StateFlow<Set<String>> =
        _generatingChats

    fun provider(id: String): Flow<ProviderEntity?> =
        container.providerRepository.provider(id)

    fun chat(id: String): Flow<ChatEntity?> =
        container.chatRepository.chat(id)

    fun messages(chatId: String): Flow<List<MessageEntity>> =
        container.chatRepository.messages(chatId)

    fun completeOnboarding() =
        viewModelScope.launch {
            container.settingsRepository.setOnboarded(true)
        }

    fun setTheme(mode: ThemeMode) =
        viewModelScope.launch {
            container.settingsRepository.setTheme(mode)
        }

    fun setLanguage(language: AppLanguage) =
        viewModelScope.launch {
            container.settingsRepository.setLanguage(language)
        }

    fun loadProviderDraft(
        id: String,
        onLoaded: (ProviderDraft?) -> Unit
    ) {
        viewModelScope.launch {
            val p = container.providerRepository.get(id)

            if (p == null) {
                onLoaded(null)
                return@launch
            }

            onLoaded(
                ProviderDraft(
                    id = p.id,
                    name = p.name,
                    protocol = runCatching {
                        ProviderProtocol.valueOf(p.protocol)
                    }.getOrDefault(
                        ProviderProtocol.OPENAI_COMPATIBLE
                    ),
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

    fun saveProvider(
        draft: ProviderDraft,
        onResult: (Result<String>) -> Unit
    ) {
        viewModelScope.launch {
            val validation = validateProviderDraft(draft)

            if (validation != null) {
                onResult(
                    Result.failure(
                        IllegalArgumentException(validation)
                    )
                )
                return@launch
            }

            runCatching {
                container.providerRepository.save(draft)
            }.also(onResult)
        }
    }

    /*
     * Connection test:
     *
     * 1. Try model discovery first.
     * 2. Some OpenAI-compatible servers do not implement /models.
     * 3. In that case, if a model has been entered, perform a tiny real
     *    non-streaming chat request.
     */
    fun testProvider(
        draft: ProviderDraft,
        onResult: (Result<List<String>>) -> Unit
    ) {
        viewModelScope.launch {
            if (draft.baseUrl.isBlank()) {
                onResult(
                    Result.failure(
                        IllegalArgumentException(
                            "Base URL is required"
                        )
                    )
                )
                return@launch
            }

            if (!draft.baseUrl.startsWith("https://") &&
                !draft.baseUrl.startsWith("http://")
            ) {
                onResult(
                    Result.failure(
                        IllegalArgumentException(
                            "Base URL must start with http:// or https://"
                        )
                    )
                )
                return@launch
            }

            if (!isValidHeadersJson(draft.extraHeadersJson)) {
                onResult(
                    Result.failure(
                        IllegalArgumentException(
                            "Invalid headers JSON"
                        )
                    )
                )
                return@launch
            }

            if (requiresApiKey(draft) &&
                draft.apiKey.isBlank()
            ) {
                onResult(
                    Result.failure(
                        IllegalArgumentException(
                            "API key is required for this provider"
                        )
                    )
                )
                return@launch
            }

            val now = System.currentTimeMillis()

            val temp = ProviderEntity(
                id = draft.id ?: "connection-test",
                name = draft.name,
                protocol = draft.protocol.name,
                baseUrl = draft.baseUrl
                    .trim()
                    .trimEnd('/'),
                model = draft.model.trim(),
                endpointPath = draft.endpointPath
                    .trim()
                    .ifBlank { "/chat/completions" },
                authHeader = draft.authHeader
                    .trim()
                    .ifBlank { "Authorization" },
                authPrefix = draft.authPrefix,
                extraHeadersJson = draft.extraHeadersJson
                    .trim()
                    .ifBlank { "{}" },
                temperature = draft.temperature
                    .coerceIn(0.0, 2.0),
                maxTokens = draft.maxTokens
                    .coerceIn(1, 262144),
                timeoutSeconds = draft.timeoutSeconds
                    .coerceIn(5, 300),
                stream = draft.stream,
                createdAt = now,
                updatedAt = now
            )

            val gateway =
                container.gatewayFactory.create(
                    draft.protocol
                )

            val modelDiscovery =
                runCatching {
                    gateway.listModels(
                        temp,
                        draft.apiKey.trim()
                    )
                }

            if (modelDiscovery.isSuccess) {
                onResult(
                    Result.success(
                        modelDiscovery.getOrThrow()
                    )
                )
                return@launch
            }

            /*
             * If no model was entered, we cannot perform a chat probe.
             * Return the original model-discovery error.
             */
            if (draft.model.isBlank()) {
                val originalError =
                    modelDiscovery.exceptionOrNull()
                        ?: IllegalStateException(
                            "Model discovery failed"
                        )

                onResult(
                    Result.failure(
                        FriendlyApiException(
                            friendlyError(originalError)
                        )
                    )
                )
                return@launch
            }

            /*
             * Tiny fallback probe for providers that support chat but do not
             * expose /models.
             */
            val probeProvider = temp.copy(
                stream = false,
                maxTokens = minOf(
                    temp.maxTokens,
                    8
                )
            )

            runCatching {
                gateway.streamChat(
                    provider = probeProvider,
                    apiKey = draft.apiKey.trim(),
                    systemPrompt = "",
                    messages = listOf(
                        ChatTurn(
                            role = "user",
                            content = "Reply with OK."
                        )
                    )
                ).first()

                /*
                 * Empty list means:
                 * connection works, but model discovery is unavailable.
                 */
                emptyList<String>()
            }.recoverCatching {
                throw FriendlyApiException(
                    friendlyError(it)
                )
            }.also(onResult)
        }
    }

    fun deleteProvider(
        provider: ProviderEntity,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            container.providerRepository.delete(provider)
            onDone()
        }
    }

    fun createChat(
        providerId: String,
        systemPrompt: String,
        onCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            val id =
                container.chatRepository.createChat(
                    providerId,
                    systemPrompt.trim()
                )

            onCreated(id)
        }
    }

    fun deleteChat(chat: ChatEntity) {
        stopGeneration(chat.id)

        viewModelScope.launch {
            container.chatRepository.delete(chat)
        }
    }

    fun sendMessage(
        chatId: String,
        text: String
    ) {
        val clean = text.trim()

        if (clean.isEmpty()) return

        if (
            generationJobs[chatId]
                ?.isActive == true
        ) {
            return
        }

        val job =
            viewModelScope.launch {
                _generatingChats.value =
                    _generatingChats.value + chatId

                var assistantId: String? = null
                var assistantText = ""

                try {
                    val chat =
                        container.chatRepository
                            .getChat(chatId)
                            ?: throw IllegalStateException(
                                "Chat not found"
                            )

                    val provider =
                        container.providerRepository
                            .get(chat.providerId)
                            ?: throw IllegalStateException(
                                "Provider not found"
                            )

                    val key =
                        container.providerRepository
                            .apiKey(provider.id)

                    /*
                     * Do not even open a network connection for cloud providers
                     * when the key is known to be missing.
                     */
                    val draftForValidation =
                        ProviderDraft(
                            id = provider.id,
                            name = provider.name,
                            protocol = runCatching {
                                ProviderProtocol.valueOf(
                                    provider.protocol
                                )
                            }.getOrDefault(
                                ProviderProtocol.OPENAI_COMPATIBLE
                            ),
                            baseUrl = provider.baseUrl,
                            apiKey = key,
                            model = provider.model
                        )

                    if (
                        requiresApiKey(
                            draftForValidation
                        ) &&
                        key.isBlank()
                    ) {
                        throw IllegalArgumentException(
                            "API key is missing for this provider"
                        )
                    }

                    /*
                     * Save the user's message first.
                     */
                    container.chatRepository
                        .addMessage(
                            chatId,
                            "user",
                            clean
                        )

                    /*
                     * Build a bounded context instead of sending the whole
                     * lifetime of the conversation every time.
                     */
                    val history =
                        buildContext(
                            container.chatRepository
                                .getMessages(chatId)
                        )

                    /*
                     * Placeholder is required so streamed deltas can update one
                     * stable database row.
                     */
                    assistantId =
                        container.chatRepository
                            .addMessage(
                                chatId,
                                "assistant",
                                ""
                            )

                    val protocol =
                        runCatching {
                            ProviderProtocol.valueOf(
                                provider.protocol
                            )
                        }.getOrDefault(
                            ProviderProtocol.OPENAI_COMPATIBLE
                        )

                    val gateway =
                        container.gatewayFactory
                            .create(protocol)

                    gateway.streamChat(
                        provider,
                        key,
                        chat.systemPrompt,
                        history
                    ).collect { delta ->
                        assistantText += delta

                        container.chatRepository
                            .updateMessage(
                                assistantId,
                                assistantText,
                                false
                            )
                    }

                    if (assistantText.isBlank()) {
                        container.chatRepository
                            .updateMessage(
                                assistantId,
                                friendlyError(
                                    IllegalStateException(
                                        "Empty model response"
                                    )
                                ),
                                true
                            )
                    }
                } catch (
                    cancel: CancellationException
                ) {
                    /*
                     * If Stop was pressed before any text arrived, remove the
                     * empty assistant bubble.
                     *
                     * If some text already arrived, preserve the partial
                     * response instead of deleting useful content.
                     */
                    if (
                        assistantId != null &&
                        assistantText.isBlank()
                    ) {
                        container.chatRepository
                            .deleteMessage(
                                assistantId
                            )
                    }

                    throw cancel
                } catch (t: Throwable) {
                    val msg =
                        friendlyError(t)

                    if (assistantId == null) {
                        container.chatRepository
                            .addMessage(
                                chatId,
                                "assistant",
                                msg,
                                true
                            )
                    } else {
                        container.chatRepository
                            .updateMessage(
                                assistantId,
                                msg,
                                true
                            )
                    }
                } finally {
                    _generatingChats.value =
                        _generatingChats.value - chatId

                    generationJobs.remove(chatId)
                }
            }

        generationJobs[chatId] = job
    }

    fun stopGeneration(chatId: String) {
        generationJobs
            .remove(chatId)
            ?.cancel()

        _generatingChats.value =
            _generatingChats.value - chatId
    }

    /*
     * Keeps the most recent useful turns only.
     *
     * This is not exact token counting, because every provider/model uses a
     * different tokenizer. It is nevertheless much safer than continuously
     * sending an unlimited chat history.
     */
    private fun buildContext(
        messages: List<MessageEntity>
    ): List<ChatTurn> {
        val selected =
            ArrayDeque<ChatTurn>()

        var characters = 0

        for (
            message in messages.asReversed()
        ) {
            if (message.isError) continue
            if (message.content.isBlank()) continue

            if (
                selected.size >=
                MAX_CONTEXT_MESSAGES
            ) {
                break
            }

            val length =
                message.content.length

            if (
                selected.isNotEmpty() &&
                characters + length >
                MAX_CONTEXT_CHARS
            ) {
                break
            }

            selected.addFirst(
                ChatTurn(
                    role = message.role,
                    content = message.content
                )
            )

            characters += length
        }

        return selected.toList()
    }

    private fun validateProviderDraft(
        draft: ProviderDraft
    ): String? {
        if (
            draft.name.isBlank() ||
            draft.baseUrl.isBlank() ||
            draft.model.isBlank()
        ) {
            return "Required fields are missing"
        }

        if (
            !draft.baseUrl.startsWith(
                "https://"
            ) &&
            !draft.baseUrl.startsWith(
                "http://"
            )
        ) {
            return "Base URL must start with http:// or https://"
        }

        if (
            requiresApiKey(draft) &&
            draft.apiKey.isBlank()
        ) {
            return "API key is required for this provider"
        }

        if (
            !isValidHeadersJson(
                draft.extraHeadersJson
            )
        ) {
            return "Invalid headers JSON"
        }

        return null
    }

    /*
     * Native cloud APIs definitely require credentials.
     *
     * OPENAI_COMPATIBLE is intentionally more flexible because this protocol
     * is also used for Ollama, LM Studio and self-hosted servers.
     *
     * Known hosted presets still require a key.
     */
    private fun requiresApiKey(
        draft: ProviderDraft
    ): Boolean {
        when (draft.protocol) {
            ProviderProtocol.OPENAI_RESPONSES,
            ProviderProtocol.ANTHROPIC,
            ProviderProtocol.GEMINI -> {
                return true
            }

            ProviderProtocol.OPENAI_COMPATIBLE -> {
                // Continue below.
            }
        }

        val host =
            runCatching {
                URI(
                    draft.baseUrl.trim()
                ).host?.lowercase()
            }.getOrNull()
                ?: return false

        val hostedDomains =
            listOf(
                "openrouter.ai",
                "groq.com",
                "mistral.ai",
                "together.xyz",
                "deepseek.com",
                "x.ai",
                "openai.com"
            )

        return hostedDomains.any { domain ->
            host == domain ||
                host.endsWith(
                    ".$domain"
                )
        }
    }

    private fun isValidHeadersJson(
        value: String
    ): Boolean {
        if (value.isBlank()) return true

        return runCatching {
            Json.parseToJsonElement(value)
        }.getOrNull()
            is kotlinx.serialization.json.JsonObject
    }

    private fun friendlyError(
        t: Throwable
    ): String {
        if (t is FriendlyApiException) {
            return t.message.orEmpty()
        }

        val s =
            textsFor(
                settings.value.language
            )

        if (t is ApiException) {
            val code =
                t.statusCode

            return when {
                code == 400 -> buildString {
                    append(s.errorGeneric)

                    val body =
                        t.responseBody
                            ?.trim()
                            ?.take(700)

                    if (!body.isNullOrBlank()) {
                        append("\n\n")
                        append(body)
                    }
                }

                code == 401 ->
                    s.errorUnauthorized

                code == 403 ->
                    s.errorForbidden

                code == 404 ->
                    s.errorNotFound

                code == 408 ->
                    s.errorNetwork

                code == 429 ->
                    s.errorRateLimit

                code != null &&
                    code in 500..599 ->
                    s.errorServer

                else ->
                    buildString {
                        append(s.errorGeneric)

                        if (code != null) {
                            append(
                                " HTTP $code."
                            )
                        }

                        val body =
                            t.responseBody
                                ?.trim()
                                ?.take(700)

                        if (!body.isNullOrBlank()) {
                            append("\n\n")
                            append(body)
                        }
                    }
            }
        }

        return when (t) {
            is IOException ->
                s.errorNetwork

            is IllegalArgumentException ->
                t.message
                    ?: s.errorGeneric

            else ->
                t.message
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: s.errorGeneric
        }
    }
}

private class FriendlyApiException(
    message: String
) : Exception(message)

class MainViewModelFactory(
    private val container: AppContainer
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (
            modelClass.isAssignableFrom(
                MainViewModel::class.java
            )
        ) {
            return MainViewModel(
                container
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}
