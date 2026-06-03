package com.board2notes.app.data.backend

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.board2notes.app.domain.model.BoardDetectionResult
import com.board2notes.app.domain.model.BooleanMask
import com.board2notes.app.domain.model.PointF2
import com.board2notes.app.domain.model.Quad
import com.board2notes.app.domain.model.RectBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

data class BackendPipelineResult(
    val jobId: String,
    val title: String,
    val body: String,
    val ocrText: String,
    val whiteCanvasBitmap: Bitmap?,
    val ocrBitmap: Bitmap?,
    val cropBitmap: Bitmap?,
    val overlayBitmap: Bitmap?,
    val warnings: List<String>,
    val timingsMs: Map<String, Double>
)

class BackendBoard2NotesClient {
    suspend fun detectBoard(
        bitmap: Bitmap,
        baseUrl: String,
        threshold: Float
    ): BoardDetectionResult = withContext(Dispatchers.IO) {
        val root = normalizeBaseUrl(baseUrl)
        val started = System.currentTimeMillis()
        val response = postModel1Detect(root, bitmap, threshold)
        val artifacts = response.optJSONObject("artifacts") ?: JSONObject()
        val overlay = fetchBitmap(root, artifacts.optString("overlay")) ?: bitmap
        val crop = fetchBitmap(root, artifacts.optString("perspective_crop")) ?: bitmap
        val mask = fetchBitmap(root, artifacts.optString("mask"))
        BoardDetectionResult(
            originalImage = bitmap,
            mask = BooleanMask(1, 1, booleanArrayOf(false)),
            maskBitmap = mask ?: Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888),
            overlayBitmap = overlay,
            cropBitmap = crop,
            boundingBox = response.optJSONObject("bbox").toRectBox(bitmap.width, bitmap.height),
            quad = response.optJSONObject("quad").toQuad(bitmap.width, bitmap.height),
            threshold = threshold,
            elapsedMs = System.currentTimeMillis() - started,
            warnings = response.optJSONArray("warnings").toStringList()
        )
    }

    suspend fun runPipeline(
        bitmap: Bitmap,
        baseUrl: String,
        threshold: Float,
        runOcr: Boolean
    ): BackendPipelineResult = withContext(Dispatchers.IO) {
        val root = normalizeBaseUrl(baseUrl)
        val response = postPipeline(root, bitmap, threshold, runOcr)
        parsePipelineResult(root, response)
    }

    suspend fun runPipelineFromQuad(
        bitmap: Bitmap,
        quad: Quad,
        baseUrl: String,
        runOcr: Boolean
    ): BackendPipelineResult = withContext(Dispatchers.IO) {
        val root = normalizeBaseUrl(baseUrl)
        val response = postPipelineFromQuad(root, bitmap, quad, runOcr)
        parsePipelineResult(root, response)
    }

    /** Tek bir görseli backend üzerinden Qwen 2.5 VL'a OCR yaptırır ("metne çevir"). */
    suspend fun ocrImageWithQwen(
        bitmap: Bitmap,
        baseUrl: String,
        jobId: String? = null
    ): String = withContext(Dispatchers.IO) {
        val root = normalizeBaseUrl(baseUrl)
        val fields = jobId?.takeIf { it.isNotBlank() }?.let { listOf("job_id" to it) }.orEmpty()
        val response = postMultipart(root, "/api/v1/ocr/qwen", bitmap, fields)
        response.optString("text")
    }

    /** Birleştirilmiş OCR metnini backend üzerinden Gemma'ya gönderip açıklama alır. */
    suspend fun explainNote(
        text: String,
        baseUrl: String,
        noteTitle: String? = null
    ): String = withContext(Dispatchers.IO) {
        val root = normalizeBaseUrl(baseUrl)
        val payload = JSONObject().apply {
            put("text", text)
            if (!noteTitle.isNullOrBlank()) put("note_title", noteTitle)
        }
        val response = postJson(root, "/api/v1/explain", payload)
        response.optString("explanation")
    }

    internal fun normalizeBaseUrl(baseUrl: String): String {
        val root = baseUrl.trim().trimEnd('/')
        require(root.startsWith("http://") || root.startsWith("https://")) {
            "Backend URL http:// veya https:// ile başlamalı."
        }
        return root
    }

    internal fun artifactUrl(baseUrl: String, path: String): String {
        return if (path.startsWith("http://") || path.startsWith("https://")) path else "${normalizeBaseUrl(baseUrl)}$path"
    }

    private fun postPipeline(root: String, bitmap: Bitmap, threshold: Float, runOcr: Boolean): JSONObject {
        val fields = listOf("threshold" to threshold.toString(), "run_ocr" to runOcr.toString())
        return postMultipart(root, "/api/v1/pipeline", bitmap, fields)
    }

    private fun postModel1Detect(root: String, bitmap: Bitmap, threshold: Float): JSONObject {
        val fields = listOf("threshold" to threshold.toString())
        return postMultipart(root, "/api/v1/model1/detect", bitmap, fields)
    }

    private fun postPipelineFromQuad(root: String, bitmap: Bitmap, quad: Quad, runOcr: Boolean): JSONObject {
        val fields = listOf(
            "quad" to quad.toBackendJson().toString(),
            "top_left_x" to quad.topLeft.x.toString(),
            "top_left_y" to quad.topLeft.y.toString(),
            "top_right_x" to quad.topRight.x.toString(),
            "top_right_y" to quad.topRight.y.toString(),
            "bottom_right_x" to quad.bottomRight.x.toString(),
            "bottom_right_y" to quad.bottomRight.y.toString(),
            "bottom_left_x" to quad.bottomLeft.x.toString(),
            "bottom_left_y" to quad.bottomLeft.y.toString(),
            "run_ocr" to runOcr.toString()
        )
        return postMultipart(root, "/api/v1/pipeline/from-quad", bitmap, fields)
    }

    private fun postMultipart(root: String, path: String, bitmap: Bitmap, fields: List<Pair<String, String>>): JSONObject {
        val boundary = "B2N-${UUID.randomUUID()}"
        val connection = (URL("$root$path").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 180_000
            doOutput = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        }

        connection.outputStream.buffered().use { output ->
            fun write(text: String) = output.write(text.toByteArray(Charsets.UTF_8))
            fun field(name: String, value: String) {
                write("--$boundary\r\n")
                write("Content-Disposition: form-data; name=\"$name\"\r\n\r\n")
                write(value)
                write("\r\n")
            }
            fields.forEach { (name, value) -> field(name, value) }
            write("--$boundary\r\n")
            write("Content-Disposition: form-data; name=\"image\"; filename=\"board.jpg\"\r\n")
            write("Content-Type: image/jpeg\r\n\r\n")
            output.write(bitmap.toJpegBytes())
            write("\r\n--$boundary--\r\n")
        }

        val status = connection.responseCode
        val body = if (status in 200..299) {
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } else {
            connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        }
        connection.disconnect()
        if (status !in 200..299) {
            throw IllegalStateException("Backend pipeline HTTP $status: $body")
        }
        return JSONObject(body)
    }

    /** JSON gövdeli POST; [postMultipart] ile aynı bağlantı/hata kalıbını izler. */
    private fun postJson(root: String, path: String, payload: JSONObject): JSONObject {
        val connection = (URL("$root$path").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 120_000
            doOutput = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
        connection.outputStream.buffered().use { output ->
            output.write(payload.toString().toByteArray(Charsets.UTF_8))
        }
        val status = connection.responseCode
        val body = if (status in 200..299) {
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } else {
            connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        }
        connection.disconnect()
        if (status !in 200..299) {
            // Backend hata gövdesi FastAPI {"detail": "..."} biçiminde gelir; varsa onu yüzeye çıkar.
            val detail = runCatching { JSONObject(body).optString("detail") }.getOrNull()
            val message = if (!detail.isNullOrBlank()) detail else body
            throw IllegalStateException(message.ifBlank { "Backend HTTP $status" })
        }
        return JSONObject(body)
    }

    private fun parsePipelineResult(root: String, response: JSONObject): BackendPipelineResult {
        val artifacts = response.optJSONObject("artifacts") ?: JSONObject()
        val note = response.optJSONObject("note") ?: JSONObject()
        return BackendPipelineResult(
            jobId = response.optString("job_id"),
            title = note.optString("title", "B2Note Notu"),
            body = note.optString("body"),
            ocrText = response.optString("ocr_text"),
            whiteCanvasBitmap = fetchBitmap(root, artifacts.optString("white_canvas")),
            ocrBitmap = fetchBitmap(root, artifacts.optString("ocr_enhanced")),
            cropBitmap = fetchBitmap(root, artifacts.optString("perspective_crop")),
            overlayBitmap = fetchBitmap(root, artifacts.optString("overlay")),
            warnings = response.optJSONArray("warnings").toStringList(),
            timingsMs = response.optJSONObject("timings_ms").toDoubleMap()
        )
    }

    private fun fetchBitmap(root: String, path: String): Bitmap? {
        if (path.isBlank()) return null
        val connection = (URL(artifactUrl(root, path)).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 60_000
        }
        return try {
            if (connection.responseCode !in 200..299) return null
            BitmapFactory.decodeStream(connection.inputStream)
        } finally {
            connection.disconnect()
        }
    }

    private fun Bitmap.toJpegBytes(): ByteArray {
        val output = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 94, output)
        return output.toByteArray()
    }

    private fun Quad.toBackendJson(): JSONObject = JSONObject().apply {
        put("top_left", topLeft.toBackendJson())
        put("top_right", topRight.toBackendJson())
        put("bottom_right", bottomRight.toBackendJson())
        put("bottom_left", bottomLeft.toBackendJson())
    }

    private fun PointF2.toBackendJson(): JSONObject = JSONObject().apply {
        put("x", x.toDouble())
        put("y", y.toDouble())
    }

    private fun JSONObject?.toRectBox(maxWidth: Int, maxHeight: Int): RectBox {
        if (this == null) return RectBox(0, 0, maxWidth, maxHeight)
        return RectBox(
            left = optInt("left", 0).coerceIn(0, maxWidth),
            top = optInt("top", 0).coerceIn(0, maxHeight),
            right = optInt("right", maxWidth).coerceIn(0, maxWidth),
            bottom = optInt("bottom", maxHeight).coerceIn(0, maxHeight)
        )
    }

    private fun JSONObject?.toQuad(maxWidth: Int, maxHeight: Int): Quad {
        fun point(name: String, fallbackX: Float, fallbackY: Float): PointF2 {
            val obj = this?.optJSONObject(name)
            return PointF2(
                x = obj?.optDouble("x", fallbackX.toDouble())?.toFloat()?.coerceIn(0f, maxWidth.toFloat()) ?: fallbackX,
                y = obj?.optDouble("y", fallbackY.toDouble())?.toFloat()?.coerceIn(0f, maxHeight.toFloat()) ?: fallbackY
            )
        }
        return Quad(
            topLeft = point("top_left", 0f, 0f),
            topRight = point("top_right", maxWidth.toFloat(), 0f),
            bottomRight = point("bottom_right", maxWidth.toFloat(), maxHeight.toFloat()),
            bottomLeft = point("bottom_left", 0f, maxHeight.toFloat())
        )
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return List(length()) { index -> optString(index) }.filter { it.isNotBlank() }
    }

    private fun JSONObject?.toDoubleMap(): Map<String, Double> {
        if (this == null) return emptyMap()
        return keys().asSequence().associateWith { key -> optDouble(key) }
    }
}
