package ir.hooshamoozan.chatyar.media

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

/** Files are private to Chatyar until a user explicitly exports one via SAF. */
class ImageFileStore(private val context: Context) {
    private val directory get() = File(context.filesDir, "generated_images").apply { mkdirs() }

    suspend fun save(bytes: ByteArray): File = withContext(Dispatchers.IO) {
        if (bytes.isEmpty() || bytes.size > 20_000_000) throw IOException("Invalid image size")
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: throw IOException("Provider did not return a supported image")
        val file = File(directory, "chatyar-${UUID.randomUUID()}.png")
        try {
            file.outputStream().use { output ->
                if (!bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output)) {
                    throw IOException("Image encoding failed")
                }
            }
        } finally { bitmap.recycle() }
        file
    }

    fun recent(): List<File> = directory.listFiles()
        ?.filter { it.isFile && it.extension == "png" }
        ?.sortedByDescending { it.lastModified() }
        ?.take(40).orEmpty()

    suspend fun export(file: File, uri: Uri) = withContext(Dispatchers.IO) {
        require(file.parentFile?.canonicalFile == directory.canonicalFile && file.isFile) { "Invalid image file" }
        context.contentResolver.openOutputStream(uri)?.use { output ->
            file.inputStream().use { it.copyTo(output) }
        } ?: throw IOException("Could not open destination")
    }
}
