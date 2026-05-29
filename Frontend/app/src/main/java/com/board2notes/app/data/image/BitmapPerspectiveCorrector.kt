package com.board2notes.app.data.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import com.board2notes.app.domain.model.PointF2
import com.board2notes.app.domain.model.Quad
import kotlin.math.hypot
import kotlin.math.roundToInt

object BitmapPerspectiveCorrector {
    fun correct(source: Bitmap, quad: Quad, maxOutputSide: Int = 2400): Bitmap {
        val rawWidth = maxOf(
            distance(quad.topLeft, quad.topRight),
            distance(quad.bottomLeft, quad.bottomRight)
        ).roundToInt().coerceAtLeast(1)
        val rawHeight = maxOf(
            distance(quad.topLeft, quad.bottomLeft),
            distance(quad.topRight, quad.bottomRight)
        ).roundToInt().coerceAtLeast(1)
        val scale = minOf(1f, maxOutputSide.toFloat() / maxOf(rawWidth, rawHeight))
        val width = (rawWidth * scale).roundToInt().coerceAtLeast(1)
        val height = (rawHeight * scale).roundToInt().coerceAtLeast(1)

        val src = quad.toFloatArray()
        val dst = floatArrayOf(
            0f, 0f,
            (width - 1).toFloat(), 0f,
            (width - 1).toFloat(), (height - 1).toFloat(),
            0f, (height - 1).toFloat()
        )
        val matrix = Matrix()
        matrix.setPolyToPoly(src, 0, dst, 0, 4)

        val corrected = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
        Canvas(corrected).drawBitmap(source, matrix, paint)
        return corrected
    }

    private fun distance(a: PointF2, b: PointF2): Float = hypot(a.x - b.x, a.y - b.y)
}
