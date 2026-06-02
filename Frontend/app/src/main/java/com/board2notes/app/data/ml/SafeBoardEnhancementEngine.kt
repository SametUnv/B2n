package com.board2notes.app.data.ml

import android.graphics.Bitmap
import com.board2notes.app.domain.engine.BoardEnhancementEngine
import com.board2notes.app.domain.model.EnhancementMode
import com.board2notes.app.domain.model.EnhancementResult

class SafeBoardEnhancementEngine(
    private val primary: BoardEnhancementEngine,
    private val fallback: BoardEnhancementEngine = HeuristicEnhancementEngine()
) : BoardEnhancementEngine {
    override suspend fun enhance(bitmap: Bitmap, mode: EnhancementMode): EnhancementResult =
        try {
            primary.enhance(bitmap, mode)
        } catch (error: Throwable) {
            val fallbackResult = fallback.enhance(bitmap, mode)
            fallbackResult.copy(
                warnings = fallbackResult.warnings + "Backend iyilestirme hatasi: ${error.message ?: error::class.java.simpleName}"
            )
        }
}
