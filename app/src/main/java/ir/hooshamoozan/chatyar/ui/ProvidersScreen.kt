package ir.hooshamoozan.chatyar.ui

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
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import ir.hooshamoozan.chatyar.data.ProviderEntity
import ir.hooshamoozan.chatyar.network.ProviderProtocol

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProvidersScreen(
    vm: MainViewModel,
    contentPadding: PaddingValues,
    onAdd: () -> Unit,
    onEdit: (String) -> Unit
) {
    val providers by vm.providers.collectAsStateWithLifecycle()
    val t = LocalAppText.current
    var deleteTarget by remember { mutableStateOf<ProviderEntity?>(null) }

    Scaffold(
        modifier = Modifier.padding(contentPadding),
        topBar = {
            TopAppBar(title = { Text(t.providers, fontWeight = FontWeight.Bold) })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Outlined.Add, contentDescription = t.addProvider)
            }
        }
    ) { inner ->
        if (providers.isEmpty()) {
            EmptyProviders(
                modifier = Modifier.padding(inner),
                onAdd = onAdd
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(inner),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(providers, key = { it.id }) { provider ->
                    ProviderCard(
                        provider = provider,
                        onEdit = { onEdit(provider.id) },
                        onDelete = { deleteTarget = provider }
                    )
                }
                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }

    deleteTarget?.let { provider ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(t.delete + " «${provider.name}»؟") },
            text = { Text(t.providerInUseDeleteWarning) },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteProvider(provider)
                    deleteTarget = null
                }) { Text(t.delete, color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text(t.cancel) }
            }
        )
    }
}

@Composable
private fun EmptyProviders(modifier: Modifier = Modifier, onAdd: () -> Unit) {
    val t = LocalAppText.current
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Outlined.Dns,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(52.dp)
            )
            Spacer(Modifier.height(14.dp))
            Text(t.noProvidersTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                t.noProvidersBody,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(18.dp))
            Button(onClick = onAdd, shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(t.addProvider)
            }
        }
    }
}

@Composable
private fun ProviderCard(
    provider: ProviderEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val t = LocalAppText.current
    val protocol = runCatching { ProviderProtocol.valueOf(provider.protocol) }
        .getOrDefault(ProviderProtocol.OPENAI_COMPATIBLE)
    val protocolText = when (protocol) {
        ProviderProtocol.OPENAI_RESPONSES -> t.protocolOpenAiResponses
        ProviderProtocol.OPENAI_COMPATIBLE -> t.protocolOpenAi
        ProviderProtocol.ANTHROPIC -> t.protocolAnthropic
        ProviderProtocol.GEMINI -> t.protocolGemini
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(provider.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(
                    "${provider.model.ifBlank { "—" }} • $protocolText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    provider.baseUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Outlined.Edit, contentDescription = t.edit)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = t.delete, tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
