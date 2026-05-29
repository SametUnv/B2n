package com.board2notes.app.data.image

import android.graphics.Bitmap
import android.graphics.Color
import com.board2notes.app.domain.model.BooleanMask
import com.board2notes.app.domain.model.RectBox

object BitmapMaskRenderer {
    fun maskToBitmap(mask: BooleanMask): Bitmap {
        val pixels = IntArray(mask.width * mask.height)
        for (i in pixels.indices) {
            pixels[i] = if (mask.data[i]) Color.argb(190, 0, 160, 180) else Color.TRANSPARENT
        }
        return Bitmap.createBitmap(pixels, mask.width, mask.height, Bitmap.Config.ARGB_8888)
    }

    fun overlay(original: Bitmap, mask: BooleanMask): Bitmap {
        val pixels = IntArray(original.width * original.height)
        original.getPixels(pixels, 0, original.width, 0, 0, original.width, original.height)
        for (i in pixels.indices) {
            if (mask.data[i]) {
                val base = pixels[i]
                val r = (Color.red(base) * 0.55f).toInt()
                val g = (Color.green(base) * 0.55f + 160f * 0.45f).toInt()
                val b = (Color.blue(base) * 0.55f + 180f * 0.45f).toInt()
                pixels[i] = Color.rgb(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
            }
        }
        return Bitmap.createBitmap(pixels, original.width, original.height, Bitmap.Config.ARGB_8888)
    }

    fun crop(original: Bitmap, box: RectBox): Bitmap {
        val safe = box.padded(0, original.width, original.height)
        return Bitmap.createBitmap(original, safe.left, safe.top, safe.width, safe.height)
    }
}
