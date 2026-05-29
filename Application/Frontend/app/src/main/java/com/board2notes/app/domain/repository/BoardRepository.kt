package com.board2notes.app.domain.repository

import com.board2notes.app.core.common.DataResult
import com.board2notes.app.domain.model.EnhancementResult
import com.board2notes.app.domain.model.ExportResult
import com.board2notes.app.domain.model.OcrNote
import com.board2notes.app.domain.model.VisualNote

/**
 * Contract for board processing.
 *
 * Today this is fulfilled by [com.board2notes.app.data.repository.MockBoardRepository].
 * When the FastAPI backend + OCR/AI models are ready, add a
 * RemoteBoardRepository implementing this same interface and swap it in
 * [com.board2notes.app.di.AppModule] — no ViewModel or UI changes required.
 */
interface BoardRepository {

    /** Simulates / performs image cleanup + de-glare for the board photo. */
    suspend fun enhanceImage(imageUri: String): DataResult<EnhancementResult>

    /** Produces editable OCR text from the (enhanced) board image. */
    suspend fun generateOcrNote(imageUri: String): DataResult<OcrNote>

    /** Produces a clean notebook-page representation of the board content. */
    suspend fun generateVisualNote(imageUri: String): DataResult<VisualNote>

    /** Simulates / performs export to a PDF document. */
    suspend fun exportNote(content: String, mode: com.board2notes.app.domain.model.BoardMode): DataResult<ExportResult>
}
