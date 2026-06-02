package com.board2notes.app.data.ml

import android.graphics.Bitmap
import com.board2notes.app.data.image.BitmapMaskRenderer
import com.board2notes.app.domain.engine.BoardSegmentationEngine
import com.board2notes.app.domain.model.BoardDetectionResult
import com.board2notes.app.domain.model.BooleanMask
import com.board2notes.app.domain.model.DebugArtifact
import com.board2notes.app.domain.model.PointF2
import com.board2notes.app.domain.model.Quad
import com.board2notes.app.domain.model.RectBox

class FallbackBoardSegmentationEngine : BoardSegmentationEngine {
    override suspend fun detectBoard(bitmap: Bitmap, threshold: Float): BoardDetectionResult {
        val box = RectBox(
            left = (bitmap.width * 0.08f).toInt(),
            top = (bitmap.height * 0.18f).toInt(),
            right = (bitmap.width * 0.92f).toInt(),
            bottom = (bitmap.height * 0.82f).toInt()
        )
        val data = BooleanArray(bitmap.width * bitmap.height)
        for (y in box.top until box.bottom) {
            for (x in box.left until box.right) {
                data[y * bitmap.width + x] = true
            }
        }
        val mask = BooleanMask(bitmap.width, bitmap.height, data)
        val overlay = BitmapMaskRenderer.overlay(bitmap, mask)
        val crop = BitmapMaskRenderer.crop(bitmap, box)
        val maskBitmap = BitmapMaskRenderer.maskToBitmap(mask)
        return BoardDetectionResult(
            originalImage = bitmap,
            mask = mask,
            maskBitmap = maskBitmap,
            overlayBitmap = overlay,
            cropBitmap = crop,
            boundingBox = box,
            quad = Quad(
                PointF2(box.left.toFloat(), box.top.toFloat()),
                PointF2(box.right.toFloat(), box.top.toFloat()),
                PointF2(box.right.toFloat(), box.bottom.toFloat()),
                PointF2(box.left.toFloat(), box.bottom.toFloat())
            ),
            threshold = threshold,
            elapsedMs = 0L,
            warnings = listOf("Backend tahta tespiti kullanılamadı. Geçici orta kırpma uygulandı."),
            debugArtifacts = listOf(
                DebugArtifact("fallback_overlay", overlay),
                DebugArtifact("fallback_crop", crop)
            )
        )
    }
}
