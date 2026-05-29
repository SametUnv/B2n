package com.board2notes.app.data.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

enum class NormalizationMode {
    ZeroToOne,
    ImageNet
}

data class TensorInput(
    val tensor: FloatArray,
    val meta: LetterboxMeta,
    val letterboxedBitmap: Bitmap
)

object ImagePreprocessor {
    private val imageNetMean = floatArrayOf(0.485f, 0.456f, 0.406f)
    private val imageNetStd = floatArrayOf(0.229f, 0.224f, 0.225f)

    fun letterboxToNchw(
        bitmap: Bitmap,
        targetSize: Int = 640,
        padValue: Int = 114,
        normalizationMode: NormalizationMode = NormalizationMode.ZeroToOne
    ): TensorInput {
        val meta = LetterboxMath.compute(bitmap.width, bitmap.height, targetSize)
        val output = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(Color.rgb(padValue, padValue, padValue))

        val resized = Bitmap.createScaledBitmap(bitmap, meta.resizedWidth, meta.resizedHeight, true)
        canvas.drawBitmap(resized, meta.padX.toFloat(), meta.padY.toFloat(), Paint(Paint.FILTER_BITMAP_FLAG))
        if (resized !== bitmap) resized.recycle()

        val pixels = IntArray(targetSize * targetSize)
        output.getPixels(pixels, 0, targetSize, 0, 0, targetSize, targetSize)

        val channelSize = targetSize * targetSize
        val tensor = FloatArray(3 * channelSize)
        for (i in pixels.indices) {
            val color = pixels[i]
            val rgb = floatArrayOf(
                Color.red(color) / 255f,
                Color.green(color) / 255f,
                Color.blue(color) / 255f
            )
            for (c in 0..2) {
                val value = when (normalizationMode) {
                    NormalizationMode.ZeroToOne -> rgb[c]
                    NormalizationMode.ImageNet -> (rgb[c] - imageNetMean[c]) / imageNetStd[c]
                }
                tensor[c * channelSize + i] = value
            }
        }

        return TensorInput(tensor, meta, output)
    }
}
