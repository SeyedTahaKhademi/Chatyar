package ir.hooshamoozan.chatyar.ui

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.hooshamoozan.chatyar.data.ProviderEntity
import ir.hooshamoozan.chatyar.network.ProviderProtocol
import java.io.File

/** OpenAI-compatible image generation; does not pretend all chat APIs generate images. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageGenerationScreen(vm: MainViewModel, initialProviderId: String?, onBack: () -> Unit) {
    val context = LocalContext.current
    val providers by vm.providers.collectAsStateWithLifecycle()
    val candidates = providers.filter { it.protocol == ProviderProtocol.OPENAI_COMPATIBLE.name || it.protocol == ProviderProtocol.OPENAI_RESPONSES.name }
    var providerId by remember { mutableStateOf(initialProviderId.orEmpty()) }
    val active = candidates.firstOrNull { it.id == providerId } ?: candidates.firstOrNull()
    var expanded by remember { mutableStateOf(false) }
    var prompt by remember { mutableStateOf("") }
    var model by remember(active?.id) { mutableStateOf(active?.model.orEmpty()) }
    var endpoint by remember { mutableStateOf("/images/generations") }
    var size by remember { mutableStateOf("1024x1024") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<File?>(null) }
    var recent by remember { mutableStateOf(vm.recentImages()) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        val file = result
        if (uri != null && file != null) vm.exportGeneratedImage(file, uri) { outcome ->
            Toast.makeText(context, if (outcome.isSuccess) "تصویر ذخیره شد / Saved" else outcome.exceptionOrNull()?.message.orEmpty(), Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("ساخت تصویر | Image Studio", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("با کلید API خودت تصویر بساز. این بخش مخصوص endpoint سازگار با OpenAI Images است.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box {
                        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Provider: ${active?.name ?: "افزودن Provider سازگار با OpenAI"}")
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            candidates.forEach { p ->
                                DropdownMenuItem(text = { Text(p.name) }, onClick = {
                                    providerId = p.id; model = p.model; expanded = false; error = null
                                })
                            }
                        }
                    }
                    OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("Image model ID") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp))
                    OutlinedTextField(value = prompt, onValueChange = { prompt = it }, label = { Text("Prompt / توضیح تصویر") }, minLines = 3, maxLines = 6, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp))
                    OutlinedTextField(value = endpoint, onValueChange = { endpoint = it }, label = { Text("Image endpoint") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp))
                    Text("ابعاد تصویر", style = MaterialTheme.typography.labelLarge)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("1024x1024", "1024x1536", "1536x1024").forEach { option ->
                            FilterChip(selected = size == option, onClick = { size = option }, label = { Text(option, style = MaterialTheme.typography.labelSmall) })
                        }
                    }
                    Button(
                        onClick = {
                            val provider = active ?: return@Button
                            busy = true; error = null
                            vm.generateImage(provider.id, prompt, model, size, endpoint) { outcome ->
                                busy = false
                                outcome.onSuccess { result = it; recent = vm.recentImages() }
                                    .onFailure { error = it.message ?: "Generation failed" }
                            }
                        },
                        enabled = !busy && active != null && model.isNotBlank() && prompt.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(18.dp)
                    ) {
                        if (busy) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Outlined.Photo, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (busy) "در حال ساخت تصویر..." else "Generate Image")
                    }
                }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            result?.let { file ->
                Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        FilePreview(file, Modifier.fillMaxWidth().heightIn(min = 150.dp, max = 430.dp))
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { launcher.launch(file.name) }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Outlined.Download, null)
                            Spacer(Modifier.width(8.dp))
                            Text("دانلود / ذخیره تصویر")
                        }
                    }
                }
            }
            if (recent.isNotEmpty()) {
                Text("تصاویر قبلی (ذخیره‌شده روی دستگاه)", fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(recent, key = { it.name }) { file ->
                        Card(modifier = Modifier.size(116.dp).clickable { result = file }, shape = RoundedCornerShape(16.dp)) {
                            FilePreview(file, Modifier.fillMaxSize())
                        }
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun FilePreview(file: File, modifier: Modifier = Modifier) {
    val bmp = remember(file.path) { runCatching { BitmapFactory.decodeFile(file.path) }.getOrNull() }
    if (bmp != null) Image(bitmap = bmp.asImageBitmap(), contentDescription = "Generated image", contentScale = ContentScale.Fit, modifier = modifier)
    else Box(modifier, contentAlignment = Alignment.Center) { Text("Image not available") }
}
