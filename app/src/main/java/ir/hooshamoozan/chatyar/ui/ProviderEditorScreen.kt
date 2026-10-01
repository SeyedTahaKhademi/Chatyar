package ir.hooshamoozan.chatyar.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import ir.hooshamoozan.chatyar.data.ProviderDraft
import ir.hooshamoozan.chatyar.network.ProviderPreset
import ir.hooshamoozan.chatyar.network.ProviderPresets
import ir.hooshamoozan.chatyar.network.ProviderProtocol
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProviderEditorScreen(
    vm: MainViewModel,
    providerId: String?,
    onBack: () -> Unit
) {
    val t = LocalAppText.current
    var draft by remember { mutableStateOf(ProviderDraft()) }
    var loading by remember { mutableStateOf(providerId != null) }
    var advanced by remember { mutableStateOf(false) }
    var protocolMenu by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var statusIsError by remember { mutableStateOf(false) }
    var models by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(providerId) {
        if (providerId != null) {
            vm.loadProviderDraft(providerId) { loaded ->
                if (loaded != null) draft = loaded
                loading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (providerId == null) t.addProvider else t.editProvider, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = t.cancel)
                    }
                }
            )
        }
    ) { inner ->
        if (loading) {
            Column(
                Modifier.fillMaxSize().padding(inner),
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.padding(32.dp))
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
                    Text(t.presetProviders, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProviderPresets.forEach { preset ->
                            AssistChip(
                                onClick = {
                                    draft = applyPreset(draft, preset)
                                    status = null
                                    models = emptyList()
                                },
                                label = { Text(preset.name) }
                            )
                        }
                    }
                    Spacer(Modifier.height(18.dp))

                    OutlinedTextField(
                        value = draft.name,
                        onValueChange = { draft = draft.copy(name = it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(t.providerName) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                    Spacer(Modifier.height(10.dp))

                    ExposedDropdownMenuBox(
                        expanded = protocolMenu,
                        onExpandedChange = { protocolMenu = !protocolMenu }
                    ) {
                        OutlinedTextField(
                            value = protocolLabel(draft.protocol, t),
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            label = { Text(t.providerType) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = protocolMenu) },
                            shape = RoundedCornerShape(14.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = protocolMenu,
                            onDismissRequest = { protocolMenu = false }
                        ) {
                            ProviderProtocol.entries.forEach { protocol ->
                                DropdownMenuItem(
                                    text = { Text(protocolLabel(protocol, t)) },
                                    onClick = {
                                        draft = when (protocol) {
                                            ProviderProtocol.OPENAI_RESPONSES -> draft.copy(
                                                protocol = protocol,
                                                endpointPath = "/responses",
                                                authHeader = "Authorization",
                                                authPrefix = "Bearer "
                                            )
                                            ProviderProtocol.OPENAI_COMPATIBLE -> draft.copy(
                                                protocol = protocol,
                                                endpointPath = "/chat/completions",
                                                authHeader = "Authorization",
                                                authPrefix = "Bearer "
                                            )
                                            ProviderProtocol.ANTHROPIC,
                                            ProviderProtocol.GEMINI -> draft.copy(protocol = protocol)
                                        }
                                        protocolMenu = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))

                    TechnicalField(
                        value = draft.baseUrl,
                        onValueChange = { draft = draft.copy(baseUrl = it) },
                        label = t.baseUrl,
                        placeholder = "https://api.example.com/v1"
                    )
                    Spacer(Modifier.height(10.dp))
                    TechnicalField(
                        value = draft.apiKey,
                        onValueChange = { draft = draft.copy(apiKey = it) },
                        label = t.apiKey,
                        placeholder = "sk-…",
                        password = true
                    )
                    Text(
                        t.keyMayBeEmpty,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    TechnicalField(
                        value = draft.model,
                        onValueChange = { draft = draft.copy(model = it) },
                        label = t.model,
                        placeholder = "model-id"
                    )

                    if (models.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${models.size} ${t.modelsFound}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            models.take(24).forEach { model ->
                                FilterChip(
                                    selected = draft.model == model,
                                    onClick = { draft = draft.copy(model = model) },
                                    label = { Text(model, maxLines = 1) }
                                )
                            }
                        }
                    }

                    if (draft.baseUrl.startsWith("http://")) {
                        Spacer(Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = .5f)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                t.cleartextWarning + "\n" + t.localEndpointNote,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            testing = true
                            status = null
                            statusIsError = false
                            vm.testProvider(draft) { result ->
                                testing = false
                                result.onSuccess { list ->
                                    models = list
                                    status = if (list.isEmpty()) t.noModelList else "${t.connectionOk} — ${list.size} ${t.modelsFound}"
                                    statusIsError = false
                                }.onFailure {
                                    status = it.message ?: t.errorGeneric
                                    statusIsError = true
                                }
                            }
                        },
                        enabled = !testing && draft.baseUrl.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (testing) {
                            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.height(20.dp))
                        } else {
                            Icon(Icons.Outlined.Science, contentDescription = null)
                        }
                        Spacer(Modifier.padding(4.dp))
                        Text(if (testing) t.testing else t.testConnection)
                    }

                    status?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (statusIsError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider()
                    TextButton(
                        onClick = { advanced = !advanced },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(t.advanced, modifier = Modifier.weight(1f))
                        Icon(if (advanced) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = null)
                    }

                    if (advanced) {
                        if (draft.protocol == ProviderProtocol.OPENAI_COMPATIBLE || draft.protocol == ProviderProtocol.OPENAI_RESPONSES) {
                            TechnicalField(
                                value = draft.endpointPath,
                                onValueChange = { draft = draft.copy(endpointPath = it) },
                                label = t.endpointPath,
                                placeholder = "/chat/completions"
                            )
                            Spacer(Modifier.height(10.dp))
                            TechnicalField(
                                value = draft.authHeader,
                                onValueChange = { draft = draft.copy(authHeader = it) },
                                label = t.authHeader,
                                placeholder = "Authorization"
                            )
                            Spacer(Modifier.height(10.dp))
                            TechnicalField(
                                value = draft.authPrefix,
                                onValueChange = { draft = draft.copy(authPrefix = it) },
                                label = t.authPrefix,
                                placeholder = "Bearer "
                            )
                            Spacer(Modifier.height(10.dp))
                        }

                        TechnicalField(
                            value = draft.extraHeadersJson,
                            onValueChange = { draft = draft.copy(extraHeadersJson = it) },
                            label = t.extraHeaders,
                            placeholder = "{\"X-Custom\":\"value\"}"
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            TechnicalField(
                                value = draft.temperature.toString(),
                                onValueChange = { v -> v.toDoubleOrNull()?.let { draft = draft.copy(temperature = it) } },
                                label = t.temperature,
                                modifier = Modifier.weight(1f),
                                keyboardType = KeyboardType.Decimal
                            )
                            TechnicalField(
                                value = draft.maxTokens.toString(),
                                onValueChange = { v -> v.toIntOrNull()?.let { draft = draft.copy(maxTokens = it) } },
                                label = t.maxTokens,
                                modifier = Modifier.weight(1f),
                                keyboardType = KeyboardType.Number
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        TechnicalField(
                            value = draft.timeoutSeconds.toString(),
                            onValueChange = { v -> v.toIntOrNull()?.let { draft = draft.copy(timeoutSeconds = it) } },
                            label = t.timeout,
                            keyboardType = KeyboardType.Number
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(t.streaming, style = MaterialTheme.typography.bodyLarge)
                            Switch(checked = draft.stream, onCheckedChange = { draft = draft.copy(stream = it) })
                        }
                    }

                    Spacer(Modifier.height(22.dp))
                    Button(
                        onClick = {
                            val localError = validateDraftUi(draft, t)
                            if (localError != null) {
                                status = localError
                                statusIsError = true
                            } else {
                                vm.saveProvider(draft) { result ->
                                    result.onSuccess { onBack() }
                                        .onFailure {
                                            status = it.message ?: t.errorGeneric
                                            statusIsError = true
                                        }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                        Spacer(Modifier.padding(4.dp))
                        Text(t.save, fontWeight = FontWeight.Bold)
                    }
            Spacer(Modifier.height(20.dp))
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
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label) },
        placeholder = { if (placeholder.isNotBlank()) Text(placeholder) },
        singleLine = true,
        visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Ltr),
        shape = RoundedCornerShape(14.dp)
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

private fun protocolLabel(protocol: ProviderProtocol, t: AppText): String = when (protocol) {
    ProviderProtocol.OPENAI_RESPONSES -> t.protocolOpenAiResponses
    ProviderProtocol.OPENAI_COMPATIBLE -> t.protocolOpenAi
    ProviderProtocol.ANTHROPIC -> t.protocolAnthropic
    ProviderProtocol.GEMINI -> t.protocolGemini
}

private fun validateDraftUi(draft: ProviderDraft, t: AppText): String? {
    if (draft.name.isBlank() || draft.baseUrl.isBlank()) return t.fillRequired
    if (draft.model.isBlank()) return t.modelRequired
    if (!draft.baseUrl.startsWith("https://") && !draft.baseUrl.startsWith("http://")) return t.fillRequired
    val headersOk = draft.extraHeadersJson.isBlank() || runCatching {
        Json.parseToJsonElement(draft.extraHeadersJson) is JsonObject
    }.getOrDefault(false)
    if (!headersOk) return t.invalidJson
    return null
}

