package com.board2notes.app.data.image

import android.content.Context
import android.graphics.Bitmap
import com.board2notes.app.domain.model.DebugArtifact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class DebugArtifactWriter(private val context: Context) {
    suspend fun write(artifacts: List<DebugArtifact>): List<File> = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "debug").apply { mkdirs() }
        artifacts.mapNotNull { artifact ->
            when {
                artifact.bitmap != null -> {
                    val file = File(dir, "${safeName(artifact.name)}.png")
                    FileOutputStream(file).use { out ->
                        artifact.bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    file
                }
                artifact.text != null -> {
                    val file = File(dir, "${safeName(artifact.name)}.txt")
                    file.writeText(artifact.text)
                    file
                }
                else -> null
            }
        }
    }

    private fun safeName(name: String): String =
        name.lowercase().replace(Regex("[^a-z0-9_-]+"), "_").trim('_').ifBlank { "artifact" }
}
