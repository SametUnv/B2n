package com.board2notes.app.data.repository

import com.board2notes.app.core.common.DataResult
import com.board2notes.app.data.mock.MockData
import com.board2notes.app.domain.model.BoardMode
import com.board2notes.app.domain.model.EnhancementResult
import com.board2notes.app.domain.model.ExportResult
import com.board2notes.app.domain.model.OcrNote
import com.board2notes.app.domain.model.VisualNote
import com.board2notes.app.domain.repository.BoardRepository
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Fake repository that fulfils [BoardRepository] using mock data and
 * artificial delays so the UI *feels* like real processing is happening.
 *
 * Replace this with a RemoteBoardRepository (Retrofit/Ktor → FastAPI)
 * in [com.board2notes.app.di.AppModule] when the backend is ready.
 */
class MockBoardRepository : BoardRepository {

    private fun now(): String =
        SimpleDateFormat("dd MMM yyyy • HH:mm", Locale("tr")).format(Date())

    override suspend fun enhanceImage(imageUri: String): DataResult<EnhancementResult> {
        delay(1800) // simulate model latency
        // No real processing: we hand back the same image; the UI applies a
        // "scanned" color filter to mock the enhanced look.
        return DataResult.Success(
            EnhancementResult(
                originalImageUri = imageUri,
                enhancedImageUri = imageUri
            )
        )
    }

    override suspend fun generateOcrNote(imageUri: String): DataResult<OcrNote> {
        delay(1200)
        return DataResult.Success(OcrNote(text = MockData.OCR_TEXT, createdAt = now()))
    }

    override suspend fun generateVisualNote(imageUri: String): DataResult<VisualNote> {
        delay(1200)
        return DataResult.Success(
            VisualNote(
                title = MockData.VISUAL_NOTE_TITLE,
                lines = MockData.VISUAL_NOTE_LINES,
                createdAt = now()
            )
        )
    }

    override suspend fun exportNote(content: String, mode: BoardMode): DataResult<ExportResult> {
        delay(900)
        val prefix = if (mode == BoardMode.OCR_NOTE) "ocr_not" else "gorsel_not"
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return DataResult.Success(
            ExportResult(
                fileName = "${prefix}_$stamp.pdf",
                createdAt = now(),
                mode = mode
            )
        )
    }
}
