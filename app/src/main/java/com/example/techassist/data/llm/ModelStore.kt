package com.example.techassist.data.llm

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

object GgufCheck {
    /** Every GGUF file starts with the 4 ASCII bytes "GGUF". */
    fun hasMagic(file: File): Boolean = try {
        file.inputStream().use { input ->
            val head = ByteArray(4)
            input.read(head) == 4 && String(head, Charsets.US_ASCII) == "GGUF"
        }
    } catch (e: IOException) {
        false
    }
}

class ModelStore(private val context: Context) {
    private val dir: File get() = File(context.filesDir, "models").apply { mkdirs() }

    fun modelFile(): File = File(dir, "model.gguf")
    fun exists(): Boolean = modelFile().isFile
    fun sizeMb(): Long = if (exists()) modelFile().length() / (1024 * 1024) else 0L

    /** Copies the picked file into app storage and rejects anything that isn't a GGUF. */
    suspend fun importFrom(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        val temp = File(dir, "model.gguf.part")
        try {
            val input = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(IOException("Could not open the selected file."))
            input.use { i -> temp.outputStream().use { o -> i.copyTo(o, 1 shl 20) } }

            if (!GgufCheck.hasMagic(temp)) {
                temp.delete()
                return@withContext Result.failure(IOException("That file is not a GGUF model."))
            }
            val dest = modelFile()
            dest.delete()
            if (!temp.renameTo(dest)) {
                temp.delete()
                return@withContext Result.failure(IOException("Could not finalize the model file."))
            }
            Result.success(Unit)
        } catch (e: IOException) {
            temp.delete() // also cleans up after a full disk
            Result.failure(e)
        }
    }
}