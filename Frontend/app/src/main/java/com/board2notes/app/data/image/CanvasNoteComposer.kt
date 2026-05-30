package com.board2notes.app.data.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import kotlin.math.max
import kotlin.math.min

object CanvasNoteComposer {
    private const val DEFAULT_WIDTH = 1600
    private const val DEFAULT_HEIGHT = 2263
    private const val PAGE_MARGIN = 96f
    private const val INK_DELTA = 18

    fun createBlankCanvas(width: Int = DEFAULT_WIDTH, height: Int = DEFAULT_HEIGHT): Bitmap {
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.WHITE)
        }
    }

    fun composeInkOnCanvas(baseBitmap: Bitmap?, inkSource: Bitmap): Bitmap {
        val base = (baseBitmap ?: createBlankCanvas()).copy(Bitmap.Config.ARGB_8888, true)
        val inkBounds = findInkBounds(inkSource) ?: return base
        val canvas = Canvas(base)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        val sourceWidth = inkBounds.width().toFloat().coerceAtLeast(1f)
        val sourceHeight = inkBounds.height().toFloat().coerceAtLeast(1f)
        val maxWidth = base.width - PAGE_MARGIN * 2f
        val nextTop = nextAvailableTop(base)
        val maxHeight = (base.height - nextTop - PAGE_MARGIN).coerceAtLeast(base.height * 0.22f)
        val scale = min(maxWidth / sourceWidth, maxHeight / sourceHeight).coerceAtMost(1.85f)
        val drawWidth = sourceWidth * scale
        val drawHeight = sourceHeight * scale
        val left = (base.width - drawWidth) / 2f
        val top = nextTop.coerceAtMost(base.height - PAGE_MARGIN - drawHeight)

        canvas.drawBitmap(
            inkSource,
            inkBounds,
            RectF(left, top, left + drawWidth, top + drawHeight),
            paint
        )
        return base
    }

    private fun nextAvailableTop(bitmap: Bitmap): Float {
        var bottom = 0
        val step = max(1, bitmap.width / 360)
        var y = 0
        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
                if (isInk(bitmap.getPixel(x, y))) {
                    bottom = y
                    break
                }
                x += step
            }
            y += step
        }
        return max(PAGE_MARGIN, bottom + 72f)
    }

    private fun findInkBounds(bitmap: Bitmap): Rect? {
        var left = bitmap.width
        var top = bitmap.height
        var right = -1
        var bottom = -1
        val step = max(1, max(bitmap.width, bitmap.height) / 1400)
        var y = 0
        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
                if (isInk(bitmap.getPixel(x, y))) {
                    left = min(left, x)
                    top = min(top, y)
                    right = max(right, x)
                    bottom = max(bottom, y)
                }
                x += step
            }
            y += step
        }
        if (right < left || bottom < top) return null
        val margin = 12
        return Rect(
            (left - margin).coerceAtLeast(0),
            (top - margin).coerceAtLeast(0),
            (right + margin).coerceAtMost(bitmap.width - 1),
            (bottom + margin).coerceAtMost(bitmap.height - 1)
        )
    }

    private fun isInk(pixel: Int): Boolean {
        if (Color.alpha(pixel) < 24) return false
        return 255 - Color.red(pixel) > INK_DELTA ||
            255 - Color.green(pixel) > INK_DELTA ||
            255 - Color.blue(pixel) > INK_DELTA
    }
}
