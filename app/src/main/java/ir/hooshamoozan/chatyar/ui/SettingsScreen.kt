package ir.hooshamoozan.chatyar.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.hooshamoozan.chatyar.data.AppLanguage
import ir.hooshamoozan.chatyar.data.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val t = LocalAppText.current
    val uriHandler = LocalUriHandler.current
    var clearDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(t.settings, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, t.cancel) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier.fillMaxSize().padding(inner).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            SectionLabel("APP")
            SettingCard(Icons.Outlined.Palette, t.theme) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(settings.themeMode == ThemeMode.SYSTEM, { vm.setTheme(ThemeMode.SYSTEM) }, { Text(t.themeSystem) })
                    FilterChip(settings.themeMode == ThemeMode.LIGHT, { vm.setTheme(ThemeMode.LIGHT) }, { Text(t.themeLight) })
                    FilterChip(settings.themeMode == ThemeMode.DARK, { vm.setTheme(ThemeMode.DARK) }, { Text(t.themeDark) })
                }
            }
            Spacer(Modifier.height(12.dp))
            SettingCard(Icons.Outlined.Translate, t.language) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(settings.language == AppLanguage.FA, { vm.setLanguage(AppLanguage.FA) }, { Text(t.persian) })
                    FilterChip(settings.language == AppLanguage.EN, { vm.setLanguage(AppLanguage.EN) }, { Text(t.english) })
                }
            }

            Spacer(Modifier.height(26.dp))
            SectionLabel("PRIVACY")
            SettingCard(Icons.Outlined.Lock, t.privacyTitle) {
                Text(t.privacyBody, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Text(t.apiKeySecure, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                Text(t.cleartextWarning, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(26.dp))
            SectionLabel("DATA")
            Card(
                modifier = Modifier.fillMaxWidth().clickable { clearDialog = true },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (settings.language == AppLanguage.FA) "پاک کردن همه گفتگوها" else "Clear all chats", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        Text(if (settings.language == AppLanguage.FA) "همه گفتگوها برای همیشه حذف می‌شوند" else "Deletes every conversation permanently", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(Icons.Outlined.DeleteSweep, null, tint = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(Modifier.height(26.dp))
            SectionLabel("COMMUNITY")
            Card(
                modifier = Modifier.fillMaxWidth().clickable { uriHandler.openUri("https://t.me/hooshamoozan") },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Send, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.padding(6.dp))
                    Column(Modifier.weight(1f)) {
                        Text(t.telegramChannel, fontWeight = FontWeight.Bold)
                        Text(t.telegramSubtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(Icons.Outlined.OpenInNew, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(38.dp))
        }
    }

    if (clearDialog) {
        AlertDialog(
            onDismissRequest = { clearDialog = false },
            title = { Text(if (settings.language == AppLanguage.FA) "همه گفتگوها حذف شوند؟" else "Clear all chats?") },
            text = { Text(if (settings.language == AppLanguage.FA) "این عملیات قابل بازگشت نیست." else "This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { vm.clearAllChats(); clearDialog = false }) {
                    Text(t.delete, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { clearDialog = false }) { Text(t.cancel) } }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 10.dp, bottom = 10.dp))
}

@Composable
private fun SettingCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}
