package ir.hooshamoozan.chatyar.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "providers")
data class ProviderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val protocol: String,
    val baseUrl: String,
    val model: String,
    val endpointPath: String,
    val authHeader: String,
    val authPrefix: String,
    val extraHeadersJson: String,
    val temperature: Double,
    val maxTokens: Int,
    val timeoutSeconds: Int,
    val stream: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "chats",
    foreignKeys = [ForeignKey(entity = ProviderEntity::class, parentColumns = ["id"], childColumns = ["providerId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("providerId")]
)
data class ChatEntity(
    @PrimaryKey val id: String,
    val title: String,
    val providerId: String,
    val systemPrompt: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "messages",
    foreignKeys = [ForeignKey(entity = ChatEntity::class, parentColumns = ["id"], childColumns = ["chatId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("chatId")]
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val role: String,
    val content: String,
    val createdAt: Long,
    val isError: Boolean = false
)
