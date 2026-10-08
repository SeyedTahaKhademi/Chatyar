package ir.hooshamoozan.chatyar.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.hooshamoozan.chatyar.data.MessageEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    vm: MainViewModel,
    chatId: String,
    onBack: () -> Unit,
    onProviders: () -> Unit,
    onSettings: () -> Unit,
    onGenerateImage: (String?) -> Unit
) {
    val chat by vm.chat(chatId).collectAsStateWithLifecycle(initialValue = null)
    val messages by vm.messages(chatId).collectAsStateWithLifecycle(initialValue = emptyList())
    val providers by vm.providers.collectAsStateWithLifecycle()
    val generating by vm.generatingChats.collectAsStateWithLifecycle()
    val provider = providers.firstOrNull { it.id == chat?.providerId }
    val isGenerating = chatId in generating
    val t = LocalAppText.current
    val listState = rememberLazyListState()
    var input by remember { mutableStateOf("") }
    var menu by remember { mutableStateOf(false) }

    val imeHeight = WindowInsets.ime.getBottom(LocalDensity.current)
    LaunchedEffect(messages.size, messages.lastOrNull()?.content?.length, imeHeight) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { vm.stopGeneration(chatId); onBack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = t.cancel)
                    }
                },
                title = {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Text(
                                provider?.model?.ifBlank { t.model } ?: t.model,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                provider?.name.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { onGenerateImage(provider?.id) }) {
                        Icon(Icons.Outlined.Image, contentDescription = "Generate image", tint = MaterialTheme.colorScheme.primary)
                    }
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
        bottomBar = {
            ChatComposer(
                input = input,
                onInput = { input = it },
                isGenerating = isGenerating,
                onSend = {
                    val text = input.trim()
                    if (text.isNotBlank()) {
                        input = ""
                        vm.sendMessage(chatId, text)
                    }
                },
                onStop = { vm.stopGeneration(chatId) }
            )
        }
    ) { inner ->
        if (messages.isEmpty()) {
            EmptyChat(
                modifier = Modifier.fillMaxSize().padding(inner),
                providerName = provider?.name.orEmpty(),
                onSuggestion = { input = it }
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(inner),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 12.dp, bottom = 14.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    if (message.role == "user") UserMessage(message) else AssistantMessage(message)
                }
            }
        }
    }
}

@Composable
private fun EmptyChat(modifier: Modifier, providerName: String, onSuggestion: (String) -> Unit) {
    val suggestions = listOf(
        "آخرین خبرهای هوش مصنوعی این هفته چیه؟",
        "یک مفهوم سخت رو ساده توضیح بده",
        "یک نمونه Coroutine در Kotlin بنویس",
        "برای یک سفر سه‌روزه برنامه‌ریزی کن"
    )
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(86.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = .12f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                }
            }
            Spacer(Modifier.height(26.dp))
            Text("چه کمکی از دستم برمیاد؟", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(9.dp))
            Text(
                if (providerName.isBlank()) "گفتگو را با یک پیام شروع کن" else "متصل به $providerName — هر چیزی خواستی بپرس",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(28.dp))
            suggestions.forEach { suggestion ->
                Surface(
                    modifier = Modifier.padding(vertical = 5.dp).clickable { onSuggestion(suggestion) },
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Text(suggestion, modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun UserMessage(message: MessageEntity) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 64.dp, end = 18.dp, top = 7.dp, bottom = 7.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp, 22.dp, 6.dp, 22.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .72f)
        ) {
            Text(message.content, modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun AssistantMessage(message: MessageEntity) {
    val clipboard = LocalClipboardManager.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 34.dp, top = 7.dp, bottom = 5.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(11.dp), verticalAlignment = Alignment.Top) {
            Surface(
                modifier = Modifier.size(30.dp),
                shape = CircleShape,
                color = if (message.isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primary.copy(alpha = .13f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.AutoAwesome, null, modifier = Modifier.size(16.dp), tint = if (message.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                }
            }
            Text(
                text = message.content.ifBlank { "…" },
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = if (message.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground
            )
        }
        if (!message.isError && message.content.isNotBlank()) {
            IconButton(
                onClick = { clipboard.setText(AnnotatedString(message.content)) },
                modifier = Modifier.padding(start = 35.dp).size(32.dp)
            ) {
                Icon(Icons.Outlined.ContentCopy, null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .55f))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatComposer(
    input: String,
    onInput: (String) -> Unit,
    isGenerating: Boolean,
    onSend: () -> Unit,
    onStop: () -> Unit
) {
    val t = LocalAppText.current
    val bringIntoView = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            shape = RoundedCornerShape(30.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier.padding(start = 10.dp, end = 7.dp, top = 5.dp, bottom = 5.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = onInput,
                    modifier = Modifier.weight(1f)
                        .bringIntoViewRequester(bringIntoView)
                        .onFocusChanged { state ->
                            if (state.isFocused) scope.launch { delay(220); bringIntoView.bringIntoView() }
                        },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { if (input.isNotBlank() && !isGenerating) onSend() }),
                    placeholder = { Text(t.messageHint, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .7f)) },
                    maxLines = 6,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        disabledBorderColor = Color.Transparent,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                Spacer(Modifier.width(5.dp))
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (isGenerating) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primary)
                        .clickable { if (isGenerating) onStop() else if (input.isNotBlank()) onSend() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isGenerating) Icons.Outlined.Stop else Icons.Outlined.ArrowUpward,
                        contentDescription = if (isGenerating) t.stop else t.send,
                        tint = if (isGenerating) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
