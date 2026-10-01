package ir.hooshamoozan.chatyar.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.hooshamoozan.chatyar.data.ChatEntity
import ir.hooshamoozan.chatyar.data.ProviderEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsScreen(
    vm: MainViewModel,
    contentPadding: PaddingValues,
    onOpenChat: (String) -> Unit,
    onAddProvider: () -> Unit
) {
    val chats by vm.chats.collectAsStateWithLifecycle()
    val providers by vm.providers.collectAsStateWithLifecycle()
    val t = LocalAppText.current
    var newChatDialog by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<ChatEntity?>(null) }

    Scaffold(
        modifier = Modifier.padding(contentPadding),
        topBar = { TopAppBar(title = { Text(t.chats, fontWeight = FontWeight.Bold) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                if (providers.isEmpty()) onAddProvider() else newChatDialog = true
            }) { Icon(Icons.Outlined.Add, contentDescription = t.newChat) }
        }
    ) { inner ->
        if (chats.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(inner),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Outlined.ChatBubbleOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(t.noChatsTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(t.noChatsBody, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = { if (providers.isEmpty()) onAddProvider() else newChatDialog = true },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(if (providers.isEmpty()) t.addProvider else t.newChat)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(inner),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(chats, key = { it.id }) { chat ->
                    val providerName = providers.firstOrNull { it.id == chat.providerId }?.name.orEmpty()
                    ChatListCard(
                        chat = chat,
                        providerName = providerName,
                        onOpen = { onOpenChat(chat.id) },
                        onDelete = { deleteTarget = chat }
                    )
                }
                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }

    if (newChatDialog) {
        NewChatDialog(
            providers = providers,
            onDismiss = { newChatDialog = false },
            onCreate = { providerId, systemPrompt ->
                vm.createChat(providerId, systemPrompt) { id ->
                    newChatDialog = false
                    onOpenChat(id)
                }
            }
        )
    }

    deleteTarget?.let { chat ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(t.delete + " «${chat.title}»؟") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteChat(chat)
                    deleteTarget = null
                }) { Text(t.delete, color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text(t.cancel) } }
        )
    }
}

@Composable
private fun ChatListCard(
    chat: ChatEntity,
    providerName: String,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val t = LocalAppText.current
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(chat.title.ifBlank { t.newChat }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (providerName.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(providerName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = t.delete)
            }
        }
    }
}

@Composable
private fun NewChatDialog(
    providers: List<ProviderEntity>,
    onDismiss: () -> Unit,
    onCreate: (providerId: String, systemPrompt: String) -> Unit
) {
    val t = LocalAppText.current
    var selected by remember { mutableStateOf(providers.firstOrNull()?.id.orEmpty()) }
    var systemPrompt by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(t.newChat) },
        text = {
            Column {
                Text(t.chooseProvider, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                providers.forEach { provider ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { selected = provider.id }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selected == provider.id, onClick = { selected = provider.id })
                        Column {
                            Text(provider.name, fontWeight = FontWeight.Medium)
                            Text(provider.model, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = systemPrompt,
                    onValueChange = { systemPrompt = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("${t.systemPrompt} (${t.optional})") },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(14.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (selected.isNotBlank()) onCreate(selected, systemPrompt) },
                enabled = selected.isNotBlank()
            ) { Text(t.create) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(t.cancel) } }
    )
}
