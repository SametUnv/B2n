package com.board2notes.app.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream


object ImageEditUtil {

    fun rotate(context: Context, sourceUri: Uri, degrees: Float): Uri? {
        val bitmap = context.contentResolver.openInputStream(sourceUri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        } ?: return null

        val matrix = Matrix().apply { postRotate(degrees) }
        val rotated = Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
        )
        if (rotated != bitmap) bitmap.recycle()

        val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }
        val outFile = File.createTempFile("rot_", ".jpg", imagesDir)
        FileOutputStream(outFile).use { rotated.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        rotated.recycle()

        val authority = "${context.packageName}.fileprovider"
        return FileProvider.getUriForFile(context, authority, outFile)
    }
}