package com.board2notes.app.data.ml

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.board2notes.app.data.models.ModelAssetManager
import com.board2notes.app.domain.engine.BoardEnhancementEngine
import com.board2notes.app.domain.model.DebugArtifact
import com.board2notes.app.domain.model.EnhancementMode
import com.board2notes.app.domain.model.EnhancementResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.FloatBuffer
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.system.measureTimeMillis

class OnnxBoardEnhancementEngine(
    private val assetManager: ModelAssetManager,
    private val modelAssetPath: String = "models/model2_enhancement/board2notes_model2_enhancement_512_full.onnx",
    private val inputName: String = "input",
    private val restoredOutputName: String = "restored",
    private val ocrOutputName: String = "ocr_enhanced",
    private val maskOutputName: String = "text_mask_logits",
    private val tileSize: Int = 512,
    private val overlap: Int = 64,
    private val maxModelInputSide: Int = 896,
    private val maxTiles: Int = 4
) : BoardEnhancementEngine {
    private val lock = Any()
    private val environment: OrtEnvironment by lazy { OrtEnvironment.getEnvironment() }
    @Volatile private var session: OrtSession? = null

    override suspend fun enhance(bitmap: Bitmap, mode: EnhancementMode): EnhancementResult =
        withContext(Dispatchers.Default) {
            var result: EnhancementResult? = null
            val elapsed = measureTimeMillis {
                Log.i(TAG, "Enhancement requested source=${bitmap.width}x${bitmap.height} mode=$mode")
                val workingBitmap = scaleForModel(bitmap)
                if (workingBitmap !== bitmap) {
                    Log.i(TAG, "Source resized for Model2 ONNX: ${bitmap.width}x${bitmap.height} -> ${workingBitmap.width}x${workingBitmap.height}")
                }
                val session = ensureSession()
                val tiled = runTiledInference(session, workingBitmap)
                val restored = rgbFloatToBitmap(tiled.restored, workingBitmap.width, workingBitmap.height)
                val ocr = rgbFloatToBitmap(tiled.ocr, workingBitmap.width, workingBitmap.height)
                val textOnWhite = textOnWhite(tiled.ocr, tiled.mask, workingBitmap.width, workingBitmap.height)
                val warnings = buildList {
                    if (workingBitmap !== bitmap) {
                        add("Model 2 hızlı çalışma için görüntü ${workingBitmap.width}x${workingBitmap.height} boyutuna indirildi.")
                    }
                }
                result = EnhancementResult(
                    inputBitmap = bitmap,
                    enhancedBitmap = restored,
                    ocrCandidateBitmap = ocr,
                    textLayerBitmap = textOnWhite,
                    mode = mode,
                    elapsedMs = 0L,
                    warnings = warnings,
                    debugArtifacts = listOf(
                        DebugArtifact("model2_input", workingBitmap),
                        DebugArtifact("model2_restored", restored),
                        DebugArtifact("model2_ocr_enhanced", ocr),
                        DebugArtifact("model2_text_on_white", textOnWhite),
                        DebugArtifact(
                            "model2_inference",
                            text = "source=${bitmap.width}x${bitmap.height} working=${workingBitmap.width}x${workingBitmap.height} tile=$tileSize overlap=$overlap outputs=$restoredOutputName,$ocrOutputName,$maskOutputName"
                        )
                    )
                )
            }
            Log.i(TAG, "Enhancement completed in ${elapsed}ms")
            result!!.copy(elapsedMs = elapsed)
        }

    private suspend fun ensureSession(): OrtSession {
        session?.let { return it }
        Log.i(TAG, "Preparing Model2 ONNX asset: $modelAssetPath")
        lateinit var modelFile: java.io.File
        val copyElapsed = measureTimeMillis {
            modelFile = assetManager.copyAssetToInternal(modelAssetPath)
        }
        Log.i(TAG, "Model2 ONNX asset ready size=${modelFile.length()} bytes copyCheck=${copyElapsed}ms")
        synchronized(lock) {
            session?.let { return it }
            val options = OrtSession.SessionOptions().apply {
                setOptimizationLevel(OrtSession.SessionOptions.OptLevel.BASIC_OPT)
                setIntraOpNumThreads(4)
                setInterOpNumThreads(1)
                setMemoryPatternOptimization(false)
                setCPUArenaAllocator(false)
                setLoggerId(TAG)
            }
            lateinit var created: OrtSession
            val sessionElapsed = measureTimeMillis {
                created = environment.createSession(modelFile.absolutePath, options)
            }
            Log.i(TAG, "Model2 ONNX session ready in ${sessionElapsed}ms")
            session = created
            return created
        }
    }

    private fun runTiledInference(session: OrtSession, bitmap: Bitmap): TiledEnhancement {
        val width = bitmap.width
        val height = bitmap.height
        val xCoords = coords(width)
        val yCoords = coords(height)
        val tileCount = xCoords.size * yCoords.size
        check(tileCount <= maxTiles) {
            "Model 2 tile sayısı çok yüksek: $tileCount. Görüntü boyutu ${width}x${height}."
        }
        Log.i(TAG, "Running Model2 tiled inference working=${width}x${height} tiles=$tileCount")
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val restored = FloatArray(width * height * 3)
        val ocr = FloatArray(width * height * 3)
        val mask = FloatArray(width * height)
        val acc = FloatArray(width * height)

        var tileNumber = 0
        for (y0 in yCoords) {
            for (x0 in xCoords) {
                tileNumber++
                val ph = minOf(tileSize, height - y0)
                val pw = minOf(tileSize, width - x0)
                val input = buildTileTensor(pixels, width, height, x0, y0)
                lateinit var output: TileOutput
                val tileElapsed = measureTimeMillis {
                    output = runTile(session, input)
                }
                Log.d(TAG, "Tile $tileNumber/$tileCount x=$x0 y=$y0 size=${pw}x${ph} inference=${tileElapsed}ms")

                for (ty in 0 until ph) {
                    for (tx in 0 until pw) {
                        val weight = windowWeight(tx, ty, pw, ph)
                        val dstIndex = (y0 + ty) * width + (x0 + tx)
                        val tileIndex = ty * tileSize + tx
                        for (c in 0..2) {
                            val outIndex = c * width * height + dstIndex
                            restored[outIndex] += clamp01(output.restored[c][tileIndex]) * weight
                            ocr[outIndex] += clamp01(output.ocr[c][tileIndex]) * weight
                        }
                        mask[dstIndex] += sigmoid(output.maskLogits[tileIndex]) * weight
                        acc[dstIndex] += weight
                    }
                }
            }
        }

        for (i in 0 until width * height) {
            val divisor = acc[i].coerceAtLeast(1e-6f)
            mask[i] = (mask[i] / divisor).coerceIn(0f, 1f)
            for (c in 0..2) {
                val outIndex = c * width * height + i
                restored[outIndex] = (restored[outIndex] / divisor).coerceIn(0f, 1f)
                ocr[outIndex] = (ocr[outIndex] / divisor).coerceIn(0f, 1f)
            }
        }

        return TiledEnhancement(restored = restored, ocr = ocr, mask = mask)
    }

    private fun scaleForModel(bitmap: Bitmap): Bitmap {
        val largestSide = maxOf(bitmap.width, bitmap.height)
        if (largestSide <= maxModelInputSide) return bitmap
        val scale = maxModelInputSide.toFloat() / largestSide
        val width = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
        val height = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    private fun coords(length: Int): List<Int> {
        if (length <= tileSize) return listOf(0)
        val step = tileSize - overlap
        val values = mutableListOf<Int>()
        var current = 0
        while (current < length - tileSize + 1) {
            values += current
            current += step
        }
        val last = length - tileSize
        if (values.lastOrNull() != last) values += last
        return values.distinct().sorted()
    }

    private fun buildTileTensor(
        pixels: IntArray,
        width: Int,
        height: Int,
        x0: Int,
        y0: Int
    ): FloatArray {
        val tensor = FloatArray(3 * tileSize * tileSize)
        val channelSize = tileSize * tileSize
        for (ty in 0 until tileSize) {
            val sy = (y0 + ty).coerceAtMost(height - 1)
            for (tx in 0 until tileSize) {
                val sx = (x0 + tx).coerceAtMost(width - 1)
                val color = pixels[sy * width + sx]
                val idx = ty * tileSize + tx
                tensor[idx] = Color.red(color) / 255f
                tensor[channelSize + idx] = Color.green(color) / 255f
                tensor[channelSize * 2 + idx] = Color.blue(color) / 255f
            }
        }
        return tensor
    }

    @Suppress("UNCHECKED_CAST")
    private fun runTile(session: OrtSession, inputTensor: FloatArray): TileOutput {
        OnnxTensor.createTensor(
            environment,
            FloatBuffer.wrap(inputTensor),
            longArrayOf(1, 3, tileSize.toLong(), tileSize.toLong())
        ).use { tensor ->
            session.run(mapOf(inputName to tensor)).use { output ->
                val restoredValue = output[restoredOutputName].orElse(output[0]).value
                    as Array<Array<Array<FloatArray>>>
                val ocrValue = output[ocrOutputName].orElse(output[1]).value
                    as Array<Array<Array<FloatArray>>>
                val maskValue = output[maskOutputName].orElse(output[2]).value
                    as Array<Array<Array<FloatArray>>>
                return TileOutput(
                    restored = flattenChannels(restoredValue[0]),
                    ocr = flattenChannels(ocrValue[0]),
                    maskLogits = flattenSingleChannel(maskValue[0][0])
                )
            }
        }
    }

    private fun flattenChannels(value: Array<Array<FloatArray>>): Array<FloatArray> =
        Array(3) { channel ->
            FloatArray(tileSize * tileSize).also { out ->
                for (y in 0 until tileSize) {
                    val row = value[channel][y]
                    for (x in 0 until tileSize) out[y * tileSize + x] = row[x]
                }
            }
        }

    private fun flattenSingleChannel(value: Array<FloatArray>): FloatArray =
        FloatArray(tileSize * tileSize).also { out ->
            for (y in 0 until tileSize) {
                val row = value[y]
                for (x in 0 until tileSize) out[y * tileSize + x] = row[x]
            }
        }

    private fun windowWeight(x: Int, y: Int, width: Int, height: Int): Float {
        val edge = minOf(overlap, width / 2, height / 2)
        if (edge <= 0) return 1f
        fun oneDim(pos: Int, length: Int): Float {
            return when {
                pos < edge -> (pos + 1).toFloat() / (edge + 1)
                pos >= length - edge -> (length - pos).toFloat() / (edge + 1)
                else -> 1f
            }
        }
        return oneDim(x, width) * oneDim(y, height)
    }

    private fun rgbFloatToBitmap(data: FloatArray, width: Int, height: Int): Bitmap {
        val pixels = IntArray(width * height)
        val channelSize = width * height
        for (i in pixels.indices) {
            val r = (data[i] * 255f).roundToInt().coerceIn(0, 255)
            val g = (data[channelSize + i] * 255f).roundToInt().coerceIn(0, 255)
            val b = (data[channelSize * 2 + i] * 255f).roundToInt().coerceIn(0, 255)
            pixels[i] = Color.rgb(r, g, b)
        }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun textOnWhite(ocr: FloatArray, mask: FloatArray, width: Int, height: Int): Bitmap {
        val alpha = blurMask(mask, width, height)
        val pixels = IntArray(width * height)
        val channelSize = width * height
        for (i in pixels.indices) {
            val a = alpha[i]
            val r = ((1f - a) + ocr[i] * a).coerceIn(0f, 1f)
            val g = ((1f - a) + ocr[channelSize + i] * a).coerceIn(0f, 1f)
            val b = ((1f - a) + ocr[channelSize * 2 + i] * a).coerceIn(0f, 1f)
            pixels[i] = Color.rgb(
                (r * 255f).roundToInt().coerceIn(0, 255),
                (g * 255f).roundToInt().coerceIn(0, 255),
                (b * 255f).roundToInt().coerceIn(0, 255)
            )
        }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun blurMask(mask: FloatArray, width: Int, height: Int): FloatArray {
        val thresholded = FloatArray(mask.size) { if (mask[it] > 0.45f) 1f else 0f }
        val out = FloatArray(mask.size)
        for (y in 0 until height) {
            for (x in 0 until width) {
                var sum = 0f
                var count = 0
                for (dy in -2..2) {
                    val sy = y + dy
                    if (sy !in 0 until height) continue
                    for (dx in -2..2) {
                        val sx = x + dx
                        if (sx !in 0 until width) continue
                        sum += thresholded[sy * width + sx]
                        count++
                    }
                }
                out[y * width + x] = (sum / count.coerceAtLeast(1)).coerceIn(0f, 1f)
            }
        }
        return out
    }

    private fun clamp01(value: Float): Float = value.coerceIn(0f, 1f)

    private fun sigmoid(value: Float): Float = (1f / (1f + exp(-value))).coerceIn(0f, 1f)

    private companion object {
        const val TAG = "B2N-Model2"
    }

    private data class TileOutput(
        val restored: Array<FloatArray>,
        val ocr: Array<FloatArray>,
        val maskLogits: FloatArray
    )

    private data class TiledEnhancement(
        val restored: FloatArray,
        val ocr: FloatArray,
        val mask: FloatArray
    )
}
