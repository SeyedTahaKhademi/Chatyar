package ir.hooshamoozan.chatyar.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.hooshamoozan.chatyar.vpn.ChatyarVpnService
import ir.hooshamoozan.chatyar.vpn.TunnelStatus
import ir.hooshamoozan.chatyar.vpn.VpnStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VpnConnectionScreen(vm: MainViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val state by VpnStatus.state.collectAsStateWithLifecycle()
    val providers by vm.providers.collectAsStateWithLifecycle()
    var profiles by remember { mutableStateOf(vm.vpnProfiles()) }
    var name by remember { mutableStateOf("") }
    var raw by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var pendingId by remember { mutableStateOf<String?>(null) }
    var selectedProviderId by remember { mutableStateOf("") }
    var modelMenu by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val id = pendingId
        if (result.resultCode == Activity.RESULT_OK && id != null) {
            ChatyarVpnService.start(context, id)
            message = "در حال برقراری تونل…"
        } else message = "مجوز VPN تأیید نشد."
        pendingId = null
    }
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* No notification permission is required to grant VPN access. */ }

    fun connect(id: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        val consent = VpnService.prepare(context)
        if (consent != null) {
            pendingId = id
            permissionLauncher.launch(consent)
        } else {
            ChatyarVpnService.start(context, id)
            message = "در حال برقراری تونل…"
        }
    }

    val provider = providers.firstOrNull { it.id == selectedProviderId } ?: providers.firstOrNull()
    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("VPN داخلی چتیار", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back")
                } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Shield, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(9.dp))
                        Text("وضعیت تونل", fontWeight = FontWeight.Bold)
                    }
                    val statusText = when (val current = state) {
                        is TunnelStatus.Disconnected -> "قطع است"
                        is TunnelStatus.Connecting -> "در حال اتصال…"
                        is TunnelStatus.Connected -> "فعال — ترافیک چتیار از تونل عبور می‌کند"
                        is TunnelStatus.Failed -> current.description
                    }
                    Text(statusText, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (state is TunnelStatus.Connecting) LinearProgressIndicator(Modifier.fillMaxWidth())
                    if (state is TunnelStatus.Connected || state is TunnelStatus.Connecting) {
                        OutlinedButton(onClick = { ChatyarVpnService.stop(context) },
                            modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Outlined.PowerSettingsNew, null)
                            Spacer(Modifier.width(8.dp))
                            Text("قطع اتصال")
                        }
                    }
                    Text(
                        "چتیار از مجوز رسمی VPN اندروید استفاده می‌کند و فقط ترافیک خودش را تونل می‌کند. " +
                            "کانفیگ‌ها حاوی رمز هستند؛ فقط از سرورهای قابل اعتماد استفاده کن.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("وارد کردن کانفیگ", fontWeight = FontWeight.Bold)
                    Text(
                        "VLESS / VMess / Trojan / Shadowsocks / Hysteria2 / TUIC / AnyTLS / SOCKS / HTTP / sing-box JSON / Clash YAML / Base64",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(value = name, onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        label = { Text("نام پروفایل") }, shape = RoundedCornerShape(16.dp))
                    OutlinedTextField(value = raw, onValueChange = { raw = it },
                        modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 6,
                        label = { Text("لینک، JSON، YAML یا متن اشتراک") },
                        shape = RoundedCornerShape(16.dp))
                    Button(onClick = {
                        runCatching { vm.importVpnProfiles(name, raw) }.onSuccess { imported ->
                            profiles = vm.vpnProfiles()
                            raw = ""; name = ""
                            message = "${imported.size} پروفایل رمزنگاری و ذخیره شد."
                        }.onFailure { message = it.message ?: "فرمت کانفیگ معتبر نیست" }
                    }, enabled = name.isNotBlank() && raw.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.Add, null)
                        Spacer(Modifier.width(6.dp))
                        Text("ذخیره کانفیگ")
                    }
                }
            }
            Text("پروفایل‌ها", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (profiles.isEmpty()) Text("هنوز کانفیگی وارد نشده است.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            profiles.forEach { item ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Text(item.name, fontWeight = FontWeight.Bold)
                        Text(item.technology, color = MaterialTheme.colorScheme.primary)
                        Button(onClick = { connect(item.id) },
                            enabled = state !is TunnelStatus.Connecting,
                            modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Outlined.PowerSettingsNew, null)
                            Spacer(Modifier.width(8.dp))
                            Text(if ((state as? TunnelStatus.Connected)?.profileId == item.id)
                                "اتصال مجدد" else "اتصال داخلی")
                        }
                        TextButton(onClick = {
                            if ((state as? TunnelStatus.Connected)?.profileId == item.id)
                                ChatyarVpnService.stop(context)
                            vm.deleteVpnProfile(item.id)
                            profiles = vm.vpnProfiles()
                        }) { Text("حذف پروفایل", color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Language, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("تست واقعی API از مسیر تونل", fontWeight = FontWeight.Bold)
                    }
                    Text("خطای 403 همیشه ناشی از محدودیت جغرافیایی نیست. ابتدا وصل شو و بعد Provider را تست کن.",
                        style = MaterialTheme.typography.bodySmall)
                    Box {
                        OutlinedButton(onClick = { modelMenu = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(provider?.name ?: "Provider موجود نیست")
                        }
                        DropdownMenu(expanded = modelMenu, onDismissRequest = { modelMenu = false }) {
                            providers.forEach { p ->
                                DropdownMenuItem(text = { Text(p.name) }, onClick = {
                                    selectedProviderId = p.id; modelMenu = false
                                })
                            }
                        }
                    }
                    Button(onClick = {
                        provider?.let { p ->
                            testing = true
                            vm.testSavedProvider(p.id) { result ->
                                testing = false
                                message = if (result.isSuccess) "Provider از مسیر VPN پاسخ داد." else
                                    "خطای تست Provider: ${result.exceptionOrNull()?.message.orEmpty()}"
                            }
                        }
                    }, enabled = !testing && provider != null && state is TunnelStatus.Connected,
                        modifier = Modifier.fillMaxWidth()) {
                        if (testing) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Text("Test Provider")
                    }
                    TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_VPN_SETTINGS)) }) {
                        Text("تنظیمات VPN اندروید")
                    }
                }
            }
            if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(24.dp))
        }
    }
}
