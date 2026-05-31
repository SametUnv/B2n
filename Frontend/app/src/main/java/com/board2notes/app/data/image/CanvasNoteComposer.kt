package com.board2notes.app.data.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import com.board2notes.app.presentation.canvas.CanvasPage
import com.board2notes.app.presentation.canvas.CanvasElement
import com.board2notes.app.presentation.canvas.StrokeElement
import com.board2notes.app.presentation.canvas.ImageElement
import com.board2notes.app.presentation.canvas.CanvasAssets
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

    fun renderPageToBitmap(
        filesDir: java.io.File,
        noteId: String,
        page: CanvasPage,
        width: Int = DEFAULT_WIDTH,
        height: Int = DEFAULT_HEIGHT
    ): Bitmap {
        val bitmap = createBlankCanvas(width, height)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            style = Paint.Style.STROKE
        }

        page.forEach { element ->
            when (element) {
                is StrokeElement -> {
                    if (element.points.size >= 2) {
                        paint.color = element.colorArgb
                        paint.strokeWidth = element.baseWidth
                        if (element.highlighter) {
                            paint.alpha = 120 // Semi-transparent for highlighters
                        } else {
                            paint.alpha = 255
                        }
                        
                        val path = Path()
                        val first = element.points.first()
                        path.moveTo(first.x, first.y)
                        for (i in 1 until element.points.size) {
                            val pt = element.points[i]
                            path.lineTo(pt.x, pt.y)
                        }
                        canvas.drawPath(path, paint)
                    }
                }
                is ImageElement -> {
                    val file = CanvasAssets.imageFile(filesDir, noteId, element.asset)
                    if (file.exists()) {
                        val imgBmp = BitmapFactory.decodeFile(file.absolutePath)
                        if (imgBmp != null) {
                            val destRect = RectF(element.left, element.top, element.left + element.width, element.top + element.height)
                            val imgPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                            canvas.drawBitmap(imgBmp, null, destRect, imgPaint)
                        }
                    }
                }
            }
        }
        return bitmap
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
