package com.board2notes.app.core.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Helpers for obtaining a content Uri to capture a camera photo into.
 * Uses the FileProvider declared in AndroidManifest.xml.
 */
object ImageFileProvider {

    /** Creates an empty temp file in the cache dir and returns a shareable content Uri. */
    fun createImageUri(context: Context): Uri {
        val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }
        val file = File.createTempFile("board_", ".jpg", imagesDir)
        val authority = "${context.packageName}.fileprovider"
        return FileProvider.getUriForFile(context, authority, file)
    }
}
