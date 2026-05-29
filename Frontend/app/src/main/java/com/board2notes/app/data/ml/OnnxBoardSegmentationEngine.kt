package com.board2notes.app.data.ml

import android.graphics.Bitmap
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.board2notes.app.data.image.BitmapMaskRenderer
import com.board2notes.app.data.image.BitmapPerspectiveCorrector
import com.board2notes.app.data.image.ImagePreprocessor
import com.board2notes.app.data.image.MaskPostprocessor
import com.board2notes.app.data.image.MaskQuadDetector
import com.board2notes.app.data.image.MathOps
import com.board2notes.app.data.models.ModelAssetManager
import com.board2notes.app.domain.engine.BoardSegmentationEngine
import com.board2notes.app.domain.model.BoardDetectionResult
import com.board2notes.app.domain.model.DebugArtifact
import com.board2notes.app.domain.model.PointF2
import com.board2notes.app.domain.model.Quad
import com.board2notes.app.domain.model.RectBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.FloatBuffer
import kotlin.system.measureTimeMillis

class OnnxBoardSegmentationEngine(
    private val assetManager: ModelAssetManager,
    private val modelAssetPath: String = "models/model1_board_segmentation/board2notes_model1_640_full.onnx",
    private val inputName: String = "input",
    private val outputName: String = "mask_logits"
) : BoardSegmentationEngine {
    private val environment: OrtEnvironment by lazy { OrtEnvironment.getEnvironment() }
    private val session: OrtSession by lazy {
        val options = OrtSession.SessionOptions().apply {
            setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
        }
        environment.createSession(assetManager.readAssetBytes(modelAssetPath), options)
    }

    override suspend fun detectBoard(bitmap: Bitmap, threshold: Float): BoardDetectionResult =
        withContext(Dispatchers.Default) {
            var result: BoardDetectionResult? = null
            val elapsed = measureTimeMillis {
                val input = ImagePreprocessor.letterboxToNchw(bitmap)
                val logits = runModel(input.tensor)
                val probabilities640 = Array(640) { y ->
                    FloatArray(640) { x -> MathOps.sigmoid(logits[y][x]) }
                }
                val probabilities = MaskPostprocessor.unletterboxProbabilities(probabilities640, input.meta)
                val mask = MaskPostprocessor.cleanMask(bitmap.width, bitmap.height, probabilities, threshold)
                val rawBox = MaskPostprocessor.boundingBox(mask)
                val warnings = mutableListOf<String>()
                val hasUsableMask = rawBox.isValid && mask.countTrue() > bitmap.width * bitmap.height * 0.005f
                val quadDetection = if (hasUsableMask) MaskQuadDetector.detect(mask) else null
                val box = if (quadDetection != null) {
                    quadDetection.boundingBox
                } else {
                    warnings += "Tahta otomatik bulunamadı. Varsayılan orta kırpma kullanıldı; elle düzeltmeyi deneyin."
                    centerBox(bitmap.width, bitmap.height)
                }
                val quad = quadDetection?.quad ?: box.toQuad()
                if (quadDetection != null && quadDetection.confidence < 0.7f) {
                    warnings += "Tahta sınırı düşük güvenle bulundu. Köşeleri elle kontrol etmeniz önerilir."
                }

                val rawCrop = BitmapMaskRenderer.crop(bitmap, box)
                val crop = runCatching {
                    BitmapPerspectiveCorrector.correct(bitmap, quad)
                }.getOrElse {
                    warnings += "Perspektif düzeltme uygulanamadı. Düz kırpma kullanıldı."
                    rawCrop
                }
                val overlay = BitmapMaskRenderer.overlay(bitmap, mask)
                val maskBitmap = BitmapMaskRenderer.maskToBitmap(mask)

                result = BoardDetectionResult(
                    originalImage = bitmap,
                    mask = mask,
                    maskBitmap = maskBitmap,
                    overlayBitmap = overlay,
                    cropBitmap = crop,
                    boundingBox = box,
                    quad = quad,
                    threshold = threshold,
                    elapsedMs = 0L,
                    warnings = warnings,
                    debugArtifacts = listOf(
                        DebugArtifact("model1_letterbox_input", input.letterboxedBitmap),
                        DebugArtifact("model1_mask", maskBitmap),
                        DebugArtifact("model1_overlay", overlay),
                        DebugArtifact("model1_bbox_crop", rawCrop),
                        DebugArtifact("model1_perspective_crop", crop),
                        DebugArtifact("model1_quad_strategy", text = quadDetection?.strategy ?: "center_fallback")
                    )
                )
            }
            result!!.copy(elapsedMs = elapsed)
        }

    @Suppress("UNCHECKED_CAST")
    private fun runModel(inputTensor: FloatArray): Array<FloatArray> {
        OnnxTensor.createTensor(
            environment,
            FloatBuffer.wrap(inputTensor),
            longArrayOf(1, 3, 640, 640)
        ).use { tensor ->
            session.run(mapOf(inputName to tensor)).use { output ->
                val value = output[outputName].orElse(output[0]).value
                    as Array<Array<Array<FloatArray>>>
                return value[0][0]
            }
        }
    }

    private fun centerBox(width: Int, height: Int): RectBox {
        val horizontalInset = (width * 0.08f).toInt()
        val verticalInset = (height * 0.18f).toInt()
        return RectBox(horizontalInset, verticalInset, width - horizontalInset, height - verticalInset)
    }

    private fun RectBox.toQuad(): Quad = Quad(
        topLeft = PointF2(left.toFloat(), top.toFloat()),
        topRight = PointF2(right.toFloat(), top.toFloat()),
        bottomRight = PointF2(right.toFloat(), bottom.toFloat()),
        bottomLeft = PointF2(left.toFloat(), bottom.toFloat())
    )
}
