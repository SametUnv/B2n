package com.board2notes.app.data.image

import android.graphics.Bitmap
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Tahta çıktısındaki yazıları cihaz tarafında koyulaştırır. Beyaz arka planı korurken
 * gri/koyu mürekkebi siyaha doğru çeker (levels + gamma eğrisi). [factor] 0 = değişiklik yok,
 * 1 = en koyu. Renkli mürekkep (mavi/kırmızı kalem) rengini korur, yalnızca koyulaşır.
 */
object InkDarkener {

    fun darken(src: Bitmap, factor: Float): Bitmap {
        val f = factor.coerceIn(0f, 1f)
        if (f <= 0.001f) return src

        val gamma = 1.0 + 2.2 * f          // >1 orta tonları koyulaştırır
        val whitePoint = 240.0             // bu değerin üstü tam beyaz olur
        val blackPoint = (12 * f).toInt()  // hafif siyah noktası lift

        val lut = IntArray(256) { i ->
            if (i <= blackPoint) 0
            else {
                val v = (i / whitePoint).coerceIn(0.0, 1.0)
                (v.pow(gamma) * 255.0).roundToInt().coerceIn(0, 255)
            }
        }

        val width = src.width
        val height = src.height
        val pixels = IntArray(width * height)
        src.getPixels(pixels, 0, width, 0, 0, width, height)
        for (index in pixels.indices) {
            val pixel = pixels[index]
            val a = (pixel ushr 24) and 0xFF
            val r = lut[(pixel ushr 16) and 0xFF]
            val g = lut[(pixel ushr 8) and 0xFF]
            val b = lut[pixel and 0xFF]
            pixels[index] = (a shl 24) or (r shl 16) or (g shl 8) or b
        }
        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        out.setPixels(pixels, 0, width, 0, 0, width, height)
        return out
    }
}
