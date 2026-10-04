package ir.hooshamoozan.chatyar.data

import ir.hooshamoozan.chatyar.network.ProviderProtocol
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ProviderRepository(private val dao: ProviderDao, private val secretStore: SecretStore) {
    val providers: Flow<List<ProviderEntity>> = dao.observeAll()
    fun provider(id: String): Flow<ProviderEntity?> = dao.observeById(id)
    suspend fun get(id: String): ProviderEntity? = dao.getById(id)
    fun apiKey(id: String): String = secretStore.get(id)

    suspend fun save(draft: ProviderDraft): String {
        val now = System.currentTimeMillis()
        val id = draft.id ?: UUID.randomUUID().toString()
        val existing = dao.getById(id)
        dao.upsert(ProviderEntity(
            id = id,
            name = draft.name.trim(),
            protocol = draft.protocol.name,
            baseUrl = draft.baseUrl.trim().trimEnd('/'),
            model = draft.model.trim(),
            endpointPath = draft.endpointPath.trim().ifBlank { "/chat/completions" },
            authHeader = draft.authHeader.trim().ifBlank { "Authorization" },
            authPrefix = draft.authPrefix,
            extraHeadersJson = draft.extraHeadersJson.trim().ifBlank { "{}" },
            temperature = draft.temperature.coerceIn(0.0, 2.0),
            maxTokens = draft.maxTokens.coerceIn(1, 262144),
            timeoutSeconds = draft.timeoutSeconds.coerceIn(10, 180),
            stream = draft.stream,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now
        ))
        if (draft.apiKey.isNotBlank()) secretStore.put(id, draft.apiKey.trim())
        return id
    }

    suspend fun delete(provider: ProviderEntity) {
        dao.delete(provider)
        secretStore.remove(provider.id)
    }
}

data class ProviderDraft(
    val id: String? = null,
    val name: String = "",
    val protocol: ProviderProtocol = ProviderProtocol.OPENAI_COMPATIBLE,
    val baseUrl: String = "",
    val apiKey: String = "",
    val model: String = "",
    val endpointPath: String = "/chat/completions",
    val authHeader: String = "Authorization",
    val authPrefix: String = "Bearer ",
    val extraHeadersJson: String = "{}",
    val temperature: Double = 0.7,
    val maxTokens: Int = 4096,
    val timeoutSeconds: Int = 60,
    val stream: Boolean = true
)

class ChatRepository(private val chatDao: ChatDao, private val messageDao: MessageDao) {
    val chats: Flow<List<ChatEntity>> = chatDao.observeAll()
    fun chat(id: String): Flow<ChatEntity?> = chatDao.observeById(id)
    fun messages(chatId: String): Flow<List<MessageEntity>> = messageDao.observeForChat(chatId)
    suspend fun getChat(id: String): ChatEntity? = chatDao.getById(id)
    suspend fun getMessages(id: String): List<MessageEntity> = messageDao.getForChat(id)

    suspend fun createChat(providerId: String, systemPrompt: String = ""): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        chatDao.upsert(ChatEntity(id, "", providerId, systemPrompt, now, now))
        return id
    }

    suspend fun addMessage(chatId: String, role: String, content: String, isError: Boolean = false): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        messageDao.upsert(MessageEntity(id, chatId, role, content, now, isError))
        chatDao.touch(chatId, now)
        if (role == "user") {
            val chat = chatDao.getById(chatId)
            if (chat != null && chat.title.isBlank()) {
                chatDao.updateTitle(chatId, content.trim().replace("\n", " ").take(48), now)
            }
        }
        return id
    }

    suspend fun updateMessage(id: String, content: String, isError: Boolean = false) = messageDao.updateContent(id, content, isError)
    suspend fun deleteMessage(id: String) = messageDao.deleteById(id)
    suspend fun delete(chat: ChatEntity) = chatDao.delete(chat)
    suspend fun clearAllChats() = chatDao.deleteAll()
}
