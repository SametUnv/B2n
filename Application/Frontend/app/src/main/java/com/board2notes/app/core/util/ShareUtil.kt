package com.board2notes.app.core.util

import android.content.Context
import android.content.Intent

/** Simple text-sharing helper using the Android share sheet. */
object ShareUtil {
    fun shareText(context: Context, text: String, title: String = "Board2Notes notu") {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Paylaş"))
    }
}
