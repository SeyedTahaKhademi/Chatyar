package ir.hooshamoozan.chatyar.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import ir.hooshamoozan.chatyar.data.ProviderDraft
import ir.hooshamoozan.chatyar.network.ProviderPreset
import ir.hooshamoozan.chatyar.network.ProviderPresets
import ir.hooshamoozan.chatyar.network.ProviderProtocol

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderEditorScreen(vm: MainViewModel, providerId: String?, onBack: () -> Unit) {
    val t = LocalAppText.current
    var draft by remember { mutableStateOf(ProviderDraft()) }
    var loading by remember { mutableStateOf(providerId != null) }
    var testing by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var statusIsError by remember { mutableStateOf(false) }
    var models by remember { mutableStateOf<List<String>>(emptyList()) }
    var advanced by remember { mutableStateOf(false) }
    var showKey by remember { mutableStateOf(false) }

    LaunchedEffect(providerId) {
        if (providerId != null) {
            vm.loadProviderDraft(providerId) { loaded ->
                if (loaded != null) draft = loaded
                loading = false
            }
        }
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (providerId == null) t.addProvider else t.editProvider, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, t.cancel) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { inner ->
        if (loading) {
            Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(inner).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Card(
                        modifier = Modifier.size(width = 48.dp, height = 5.dp),
                        shape = RoundedCornerShape(99.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.outline)
                    ) {}
                }
                Spacer(Modifier.height(24.dp))

                Text(t.presetProviders, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProviderPresets.forEach { preset ->
                        FilterChip(
                            selected = draft.name == preset.name && draft.baseUrl == preset.baseUrl,
                            onClick = { draft = applyPreset(draft, preset); status = null; models = emptyList() },
                            label = { Text(preset.name) }
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
                TechnicalField(draft.name, { draft = draft.copy(name = it) }, t.providerName, placeholder = "My provider")
                Spacer(Modifier.height(12.dp))
                TechnicalField(draft.baseUrl, { draft = draft.copy(baseUrl = it) }, t.baseUrl, placeholder = "https://api.example.com/v1")
                Spacer(Modifier.height(12.dp))
                TechnicalField(
                    value = draft.apiKey,
                    onValueChange = { draft = draft.copy(apiKey = it) },
                    label = t.apiKey,
                    placeholder = "sk-…",
                    password = !showKey,
                    trailing = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(if (showKey) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, null)
                        }
                    }
                )
                Text(t.keyMayBeEmpty, modifier = Modifier.padding(start = 4.dp, top = 5.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(12.dp))
                TechnicalField(draft.model, { draft = draft.copy(model = it) }, t.model, placeholder = "model-id")

                if (models.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text("${models.size} ${t.modelsFound}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(7.dp))
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        models.take(40).forEach { model ->
                            FilterChip(selected = draft.model == model, onClick = { draft = draft.copy(model = model) }, label = { Text(model, maxLines = 1) })
                        }
                    }
                }

                if (draft.baseUrl.startsWith("http://")) {
                    Spacer(Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = .28f)),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text(t.cleartextWarning + "\n" + t.localEndpointNote, modifier = Modifier.padding(14.dp), style = MaterialTheme.typography.bodySmall)
                    }
                }

                Spacer(Modifier.height(18.dp))
                OutlinedButton(
                    onClick = {
                        testing = true
                        status = null
                        statusIsError = false
                        vm.testProvider(draft) { result ->
                            testing = false
                            result.onSuccess { list ->
                                models = list
                                status = if (list.isEmpty()) t.connectionOk else "${t.connectionOk} — ${list.size} ${t.modelsFound}"
                                statusIsError = false
                            }.onFailure {
                                status = it.message ?: t.errorGeneric
                                statusIsError = true
                            }
                        }
                    },
                    enabled = !testing && draft.baseUrl.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    if (testing) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    else Icon(Icons.Outlined.Science, null)
                    Spacer(Modifier.size(9.dp))
                    Text(if (testing) t.testing else t.testConnection, fontWeight = FontWeight.SemiBold)
                }

                status?.let {
                    Spacer(Modifier.height(10.dp))
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (statusIsError) MaterialTheme.colorScheme.errorContainer.copy(alpha = .35f)
                            else MaterialTheme.colorScheme.primary.copy(alpha = .10f)
                        )
                    ) {
                        Text(
                            it,
                            modifier = Modifier.fillMaxWidth().padding(13.dp),
                            color = if (statusIsError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .45f))
                TextButton(onClick = { advanced = !advanced }, modifier = Modifier.fillMaxWidth()) {
                    Text(t.advanced, modifier = Modifier.weight(1f))
                    Icon(if (advanced) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null)
                }

                if (advanced) {
                    ProtocolChips(draft.protocol) { protocol -> draft = switchProtocol(draft, protocol) }
                    Spacer(Modifier.height(12.dp))
                    if (draft.protocol == ProviderProtocol.OPENAI_COMPATIBLE || draft.protocol == ProviderProtocol.OPENAI_RESPONSES) {
                        TechnicalField(draft.endpointPath, { draft = draft.copy(endpointPath = it) }, t.endpointPath, placeholder = "/chat/completions")
                        Spacer(Modifier.height(12.dp))
                        TechnicalField(draft.authHeader, { draft = draft.copy(authHeader = it) }, t.authHeader, placeholder = "Authorization")
                        Spacer(Modifier.height(12.dp))
                        TechnicalField(draft.authPrefix, { draft = draft.copy(authPrefix = it) }, t.authPrefix, placeholder = "Bearer ")
                        Spacer(Modifier.height(12.dp))
                    }
                    TechnicalField(draft.extraHeadersJson, { draft = draft.copy(extraHeadersJson = it) }, t.extraHeaders, placeholder = "{\"X-Custom\":\"value\"}")
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TechnicalField(draft.temperature.toString(), { v -> v.toDoubleOrNull()?.let { draft = draft.copy(temperature = it) } }, t.temperature, Modifier.weight(1f), keyboardType = KeyboardType.Decimal)
                        TechnicalField(draft.maxTokens.toString(), { v -> v.toIntOrNull()?.let { draft = draft.copy(maxTokens = it) } }, t.maxTokens, Modifier.weight(1f), keyboardType = KeyboardType.Number)
                    }
                    Spacer(Modifier.height(12.dp))
                    TechnicalField(draft.timeoutSeconds.toString(), { v -> v.toIntOrNull()?.let { draft = draft.copy(timeoutSeconds = it) } }, t.timeout, keyboardType = KeyboardType.Number)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(t.streaming, fontWeight = FontWeight.SemiBold)
                            Text("SSE / live tokens", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(draft.stream, { draft = draft.copy(stream = it) })
                    }
                }

                Spacer(Modifier.height(26.dp))
                Button(
                    onClick = {
                        vm.saveProvider(draft) { result ->
                            result.onSuccess { onBack() }.onFailure {
                                status = it.message ?: t.errorGeneric
                                statusIsError = true
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(Icons.Outlined.CheckCircle, null)
                    Spacer(Modifier.size(9.dp))
                    Text(t.save, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(30.dp))
            }
        }
    }
}

@Composable
private fun ProtocolChips(selected: ProviderProtocol, onSelected: (ProviderProtocol) -> Unit) {
    val t = LocalAppText.current
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ProviderProtocol.entries.forEach { protocol ->
            val label = when (protocol) {
                ProviderProtocol.OPENAI_RESPONSES -> t.protocolOpenAiResponses
                ProviderProtocol.OPENAI_COMPATIBLE -> t.protocolOpenAi
                ProviderProtocol.ANTHROPIC -> t.protocolAnthropic
                ProviderProtocol.GEMINI -> t.protocolGemini
            }
            FilterChip(selected = selected == protocol, onClick = { onSelected(protocol) }, label = { Text(label) })
        }
    }
}

@Composable
private fun TechnicalField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    placeholder: String = "",
    password: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    trailing: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label) },
        placeholder = { if (placeholder.isNotBlank()) Text(placeholder) },
        singleLine = true,
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        trailingIcon = trailing,
        shape = RoundedCornerShape(20.dp)
    )
}

private fun applyPreset(current: ProviderDraft, preset: ProviderPreset): ProviderDraft = current.copy(
    name = preset.name,
    protocol = preset.protocol,
    baseUrl = preset.baseUrl,
    model = preset.model,
    endpointPath = preset.endpointPath,
    authHeader = preset.authHeader,
    authPrefix = preset.authPrefix
)

private fun switchProtocol(current: ProviderDraft, protocol: ProviderProtocol): ProviderDraft = when (protocol) {
    ProviderProtocol.OPENAI_RESPONSES -> current.copy(protocol = protocol, endpointPath = "/responses", authHeader = "Authorization", authPrefix = "Bearer ")
    ProviderProtocol.OPENAI_COMPATIBLE -> current.copy(protocol = protocol, endpointPath = "/chat/completions", authHeader = "Authorization", authPrefix = "Bearer ")
    ProviderProtocol.ANTHROPIC, ProviderProtocol.GEMINI -> current.copy(protocol = protocol)
}
