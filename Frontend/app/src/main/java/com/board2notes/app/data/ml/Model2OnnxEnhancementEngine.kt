package com.board2notes.app.data.ml

import android.graphics.Bitmap
import com.board2notes.app.domain.engine.BoardEnhancementEngine
import com.board2notes.app.domain.model.EnhancementMode
import com.board2notes.app.domain.model.EnhancementResult

class Model2OnnxEnhancementEngine : BoardEnhancementEngine {
    override suspend fun enhance(bitmap: Bitmap, mode: EnhancementMode): EnhancementResult {
        error("Model 2 için ONNX/TFLite export henüz repoda yok. final_model.pt Android runtime tarafından doğrudan kullanılamaz.")
    }
}
