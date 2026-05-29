package com.board2notes.app.data.models

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ModelAssetManager(private val context: Context) {
    suspend fun copyAssetToInternal(assetPath: String): File = withContext(Dispatchers.IO) {
        val destination = File(context.filesDir, assetPath).apply {
            parentFile?.mkdirs()
        }
        if (!destination.exists() || destination.length() == 0L) {
            context.assets.open(assetPath).use { input ->
                destination.outputStream().use { output -> input.copyTo(output) }
            }
        }
        destination
    }

    fun readAssetBytes(assetPath: String): ByteArray =
        context.assets.open(assetPath).use { it.readBytes() }
}
