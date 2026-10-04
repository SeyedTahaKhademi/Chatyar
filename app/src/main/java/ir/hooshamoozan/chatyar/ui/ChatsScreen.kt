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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
    onOpenChat: (String) -> Unit,
    onAddProvider: () -> Unit,
    onProviders: () -> Unit,
    onSettings: () -> Unit
) {
    val chats by vm.chats.collectAsStateWithLifecycle()
    val providers by vm.providers.collectAsStateWithLifecycle()
    val t = LocalAppText.current
    var newChat by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<ChatEntity?>(null) }
    var menu by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(t.appName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                        Text(t.chats, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, null) }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(
                                text = { Text(t.providers) },
                                leadingIcon = { Icon(Icons.Outlined.Dns, null) },
                                onClick = { menu = false; onProviders() }
                            )
                            DropdownMenuItem(
                                text = { Text(t.settings) },
                                leadingIcon = { Icon(Icons.Outlined.Settings, null) },
                                onClick = { menu = false; onSettings() }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { if (providers.isEmpty()) onAddProvider() else newChat = true },
                shape = RoundedCornerShape(20.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Outlined.Add, null) },
                text = { Text(if (providers.isEmpty()) t.addProvider else t.newChat, fontWeight = FontWeight.SemiBold) }
            )
        }
    ) { inner ->
        if (chats.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
                Column(
                    modifier = Modifier.padding(horizontal = 34.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = .12f)) {
                        Box(Modifier.size(78.dp), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.ChatBubbleOutline, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
                        }
                    }
                    Spacer(Modifier.height(22.dp))
                    Text(t.noChatsTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(t.noChatsBody, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(inner),
                contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 110.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(chats, key = { it.id }) { chat ->
                    val provider = providers.firstOrNull { it.id == chat.providerId }
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { onOpenChat(chat.id) },
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(42.dp).then(Modifier),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(Modifier.fillMaxSize(), shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = .1f)) {}
                                Icon(Icons.Outlined.ChatBubbleOutline, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.size(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(chat.title.ifBlank { t.newChat }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    listOfNotNull(provider?.name, provider?.model).joinToString(" • "),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { deleteTarget = chat }) { Icon(Icons.Outlined.DeleteOutline, t.delete, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }
                }
            }
        }
    }

    if (newChat) {
        NewChatDialog(providers, { newChat = false }) { providerId, prompt ->
            vm.createChat(providerId, prompt) { id -> newChat = false; onOpenChat(id) }
        }
    }

    deleteTarget?.let { chat ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("${t.delete} «${chat.title.ifBlank { t.newChat }}»؟") },
            confirmButton = { TextButton(onClick = { vm.deleteChat(chat); deleteTarget = null }) { Text(t.delete, color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text(t.cancel) } }
        )
    }
}

@Composable
private fun NewChatDialog(providers: List<ProviderEntity>, onDismiss: () -> Unit, onCreate: (String, String) -> Unit) {
    val t = LocalAppText.current
    var selected by remember { mutableStateOf(providers.firstOrNull()?.id.orEmpty()) }
    var systemPrompt by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(t.newChat) },
        text = {
            Column {
                Text(t.chooseProvider, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                providers.forEach { provider ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { selected = provider.id }.padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selected == provider.id, onClick = { selected = provider.id })
                        Column {
                            Text(provider.name, fontWeight = FontWeight.Medium)
                            Text(provider.model, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
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
                    shape = RoundedCornerShape(18.dp)
                )
            }
        },
        confirmButton = { TextButton(onClick = { if (selected.isNotBlank()) onCreate(selected, systemPrompt) }, enabled = selected.isNotBlank()) { Text(t.create) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(t.cancel) } }
    )
}
