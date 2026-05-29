package com.board2notes.app.data.ml

import android.graphics.Bitmap
import android.graphics.Color
import com.board2notes.app.domain.engine.BoardEnhancementEngine
import com.board2notes.app.domain.model.DebugArtifact
import com.board2notes.app.domain.model.EnhancementMode
import com.board2notes.app.domain.model.EnhancementResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import kotlin.system.measureTimeMillis

class HeuristicEnhancementEngine : BoardEnhancementEngine {
    override suspend fun enhance(bitmap: Bitmap, mode: EnhancementMode): EnhancementResult =
        withContext(Dispatchers.Default) {
            var result: EnhancementResult? = null
            val elapsed = measureTimeMillis {
                val enhanced = enhanceContrast(bitmap)
                val ocrCandidate = createOcrCandidate(enhanced)
                val textLayer = createTextOnWhite(ocrCandidate)
                result = EnhancementResult(
                    inputBitmap = bitmap,
                    enhancedBitmap = enhanced,
                    ocrCandidateBitmap = ocrCandidate,
                    textLayerBitmap = textLayer,
                    mode = mode,
                    elapsedMs = 0L,
                    warnings = listOf("Model 2 ONNX/TFLite export hazır değil. Geçici kontrast ve eşik tabanlı iyileştirme kullanılıyor."),
                    debugArtifacts = listOf(
                        DebugArtifact("enhancement_input", bitmap),
                        DebugArtifact("enhancement_contrast", enhanced),
                        DebugArtifact("enhancement_ocr_candidate", ocrCandidate),
                        DebugArtifact("enhancement_text_layer", textLayer)
                    )
                )
            }
            result!!.copy(elapsedMs = elapsed)
        }

    private fun enhanceContrast(bitmap: Bitmap): Bitmap {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        for (i in pixels.indices) {
            val c = pixels[i]
            val r = contrast(Color.red(c))
            val g = contrast(Color.green(c))
            val b = contrast(Color.blue(c))
            pixels[i] = Color.rgb(r, g, b)
        }
        return Bitmap.createBitmap(pixels, bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    }

    private fun createOcrCandidate(bitmap: Bitmap): Bitmap {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        for (i in pixels.indices) {
            val c = pixels[i]
            val luminance = (0.299f * Color.red(c) + 0.587f * Color.green(c) + 0.114f * Color.blue(c)).roundToInt()
            val value = if (luminance < 170) 30 else 255
            pixels[i] = Color.rgb(value, value, value)
        }
        return Bitmap.createBitmap(pixels, bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    }

    private fun createTextOnWhite(bitmap: Bitmap): Bitmap {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        for (i in pixels.indices) {
            val c = pixels[i]
            val v = Color.red(c)
            pixels[i] = if (v < 180) Color.rgb(18, 18, 18) else Color.WHITE
        }
        return Bitmap.createBitmap(pixels, bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    }

    private fun contrast(value: Int): Int =
        (((value - 128) * 1.25f) + 128 + 8).roundToInt().coerceIn(0, 255)
}
