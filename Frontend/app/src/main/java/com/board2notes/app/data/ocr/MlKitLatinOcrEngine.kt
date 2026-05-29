package com.board2notes.app.data.ocr

import android.graphics.Bitmap
import com.board2notes.app.domain.engine.OcrEngine
import com.board2notes.app.domain.model.OcrLine
import com.board2notes.app.domain.model.OcrResult
import com.board2notes.app.domain.model.OcrWord
import com.board2notes.app.domain.model.RectBox
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import kotlin.system.measureTimeMillis

class MlKitLatinOcrEngine : OcrEngine {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun recognize(image: Bitmap): OcrResult {
        var recognized: Text? = null
        val elapsed = measureTimeMillis {
            recognized = recognizer.process(InputImage.fromBitmap(image, 0)).await()
        }
        val text = recognized ?: return OcrResult(
            rawText = "",
            lines = emptyList(),
            words = emptyList(),
            confidence = null,
            elapsedMs = elapsed,
            warnings = listOf("OCR sonucu boş döndü.")
        )

        val lines = text.textBlocks.flatMap { block ->
            block.lines.map { line ->
                val words = line.elements.map { element ->
                    OcrWord(
                        text = element.text,
                        boundingBox = element.boundingBox?.toRectBox(),
                        confidence = null
                    )
                }
                OcrLine(
                    text = line.text,
                    boundingBox = line.boundingBox?.toRectBox(),
                    confidence = null,
                    words = words
                )
            }
        }
        val words = lines.flatMap { it.words }
        val warnings = buildList {
            if (text.text.isBlank()) add("Metin bulunamadı. Daha net bir fotoğraf veya farklı threshold deneyin.")
            if (lines.size <= 1 && text.text.length < 20) add("OCR güveni düşük olabilir; sonucu not ekranında kontrol edin.")
        }
        return OcrResult(
            rawText = text.text,
            lines = lines,
            words = words,
            confidence = null,
            elapsedMs = elapsed,
            warnings = warnings
        )
    }

    private fun android.graphics.Rect.toRectBox(): RectBox = RectBox(left, top, right, bottom)
}
