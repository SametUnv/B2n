package com.board2notes.app.presentation.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Canvas belge modeli. Bir not, sıralı [CanvasPage] listesi içerir; her sayfa çizim
 * sırasına göre [CanvasElement] tutar. Çizgiler ve görseller aynı listede yaşar, böylece
 * z-sırası (üst üste binme) korunur.
 *
 * Belge, not gövdesi (body) içine sondaki [DOC_MARKER] işaretinden sonra JSON olarak gömülür;
 * işaretten önceki kısım kullanıcının düz metin notudur. Görsel piksel verisi gövdeye gömülmez —
 * not klasöründe PNG asset olarak saklanır ve [ImageElement.asset] dosya adıyla referanslanır.
 */
sealed interface CanvasElement {
    val id: String
}

/** Serbest el çizgisi. [pressures] boşsa tüm noktalar için 1.0 kabul edilir. */
data class StrokeElement(
    override val id: String = UUID.randomUUID().toString(),
    val colorArgb: Int,
    val baseWidth: Float,
    val points: List<Offset>,
    val pressures: List<Float> = emptyList(),
    val highlighter: Boolean = false
) : CanvasElement {
    val color: Color get() = Color(colorArgb)

    fun pressureAt(index: Int): Float =
        pressures.getOrNull(index)?.takeIf { it > 0f } ?: 1f
}

/** Canvas üzerine yerleştirilmiş, taşınabilir/boyutlandırılabilir görsel (tahta çıktısı veya galeri). */
data class ImageElement(
    override val id: String = UUID.randomUUID().toString(),
    val asset: String,
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
    val rotation: Float = 0f,
    val initialLeft: Float = left,
    val initialTop: Float = top,
    val initialWidth: Float = width,
    val initialHeight: Float = height
) : CanvasElement {
    val rect: Rect get() = Rect(left, top, left + width, top + height)
    val center: Offset get() = Offset(left + width / 2f, top + height / 2f)
}

typealias CanvasPage = List<CanvasElement>

object CanvasPaper {
    const val WIDTH = 3200f
    const val HEIGHT = 4525f
    const val LEGACY_WIDTH = 1080f
    const val LEGACY_HEIGHT = 1528f
}

data class CanvasDocument(
    val text: String,
    val pages: List<CanvasPage>
) {
    fun bodyString(): String = CanvasDocumentCodec.serialize(text, pages)

    companion object {
        fun fromBody(body: String): CanvasDocument = CanvasDocumentCodec.parse(body)
        fun blank(): CanvasDocument = CanvasDocument("", listOf(emptyList()))
    }
}

/** Not klasörü ve görsel asset dosya yolları için tek kaynak. */
object CanvasAssets {
    fun noteDir(filesDir: File, noteId: String): File = File(filesDir, "notes/$noteId")

    fun imageFile(filesDir: File, noteId: String, asset: String): File =
        File(noteDir(filesDir, noteId), asset)

    fun previewFile(filesDir: File, noteId: String, pageIndex: Int = 0): File =
        File(noteDir(filesDir, noteId), if (pageIndex == 0) "canvas.png" else "canvas_page_$pageIndex.png")

    fun backgroundFile(filesDir: File, noteId: String, pageIndex: Int = 0): File =
        File(noteDir(filesDir, noteId), if (pageIndex == 0) "canvas_background.png" else "canvas_background_page_$pageIndex.png")

    fun newImageAssetName(): String = "img_${UUID.randomUUID()}.png"
}

/**
 * Belge serileştirme/ayrıştırma. Geriye dönük uyumluluk: eski `[CanvasPages:...]` ve
 * `[DrawingData:...]` biçimleri okunmaya devam eder; kayıt her zaman yeni [DOC_MARKER] biçimiyle yapılır.
 */
object CanvasDocumentCodec {
    private const val DOC_MARKER = "\n\n[[B2N_CANVAS_V2]]\n"
    private val LEGACY_PAGES_REGEX = """\n\n\[CanvasPages:(.*)\]""".toRegex(RegexOption.DOT_MATCHES_ALL)
    private val LEGACY_DRAWING_REGEX = """\n\n\[DrawingData:(.*)\]""".toRegex(RegexOption.DOT_MATCHES_ALL)

    fun hasStructuredDocument(body: String): Boolean =
        body.contains(DOC_MARKER) || LEGACY_PAGES_REGEX.containsMatchIn(body) || LEGACY_DRAWING_REGEX.containsMatchIn(body)

    fun serialize(text: String, pages: List<CanvasPage>): String {
        val safePages = pages.ifEmpty { listOf(emptyList()) }
        val pagesJson = JSONArray()
        safePages.forEach { page ->
            val pageJson = JSONArray()
            page.forEach { element -> pageJson.put(elementToJson(element)) }
            pagesJson.put(pageJson)
        }
        val root = JSONObject().apply {
            put("v", 3)
            put("paperW", CanvasPaper.WIDTH.toDouble())
            put("paperH", CanvasPaper.HEIGHT.toDouble())
            put("pages", pagesJson)
        }
        return text.trimEnd() + DOC_MARKER + root.toString()
    }

    fun parse(body: String): CanvasDocument {
        val markerIndex = body.indexOf(DOC_MARKER)
        if (markerIndex >= 0) {
            val text = body.substring(0, markerIndex)
            val json = body.substring(markerIndex + DOC_MARKER.length)
            return CanvasDocument(text, parsePagesJson(json))
        }
        // Geriye dönük uyumluluk
        LEGACY_PAGES_REGEX.find(body)?.let { match ->
            val text = body.substring(0, match.range.first)
            val pages = match.groupValues[1]
                .split("~PAGE~")
                .map { parseLegacyPayload(it) }
                .ifEmpty { listOf(emptyList()) }
            return CanvasDocument(text, scalePagesFromPaper(pages, CanvasPaper.LEGACY_WIDTH, CanvasPaper.LEGACY_HEIGHT))
        }
        LEGACY_DRAWING_REGEX.find(body)?.let { match ->
            val text = body.substring(0, match.range.first)
            return CanvasDocument(
                text,
                scalePagesFromPaper(listOf(parseLegacyPayload(match.groupValues[1])), CanvasPaper.LEGACY_WIDTH, CanvasPaper.LEGACY_HEIGHT)
            )
        }
        return CanvasDocument(body, listOf(emptyList()))
    }

    private fun parsePagesJson(json: String): List<CanvasPage> {
        if (json.isBlank()) return listOf(emptyList())
        return runCatching {
            val trimmed = json.trim()
            if (trimmed.startsWith("{")) {
                val root = JSONObject(trimmed)
                val pages = parsePagesArray(root.optJSONArray("pages") ?: JSONArray())
                val sourceW = root.optDouble("paperW", CanvasPaper.WIDTH.toDouble()).toFloat()
                val sourceH = root.optDouble("paperH", CanvasPaper.HEIGHT.toDouble()).toFloat()
                scalePagesFromPaper(pages, sourceW, sourceH)
            } else {
                scalePagesFromPaper(parsePagesArray(JSONArray(trimmed)), CanvasPaper.LEGACY_WIDTH, CanvasPaper.LEGACY_HEIGHT)
            }
        }.getOrElse { listOf(emptyList()) }
    }

    private fun parsePagesArray(array: JSONArray): List<CanvasPage> {
        val pages = (0 until array.length()).map { pageIndex ->
            val pageJson = array.optJSONArray(pageIndex) ?: JSONArray()
            (0 until pageJson.length()).mapNotNull { elementToModel(pageJson.optJSONObject(it)) }
        }
        return pages.ifEmpty { listOf(emptyList()) }
    }

    private fun scalePagesFromPaper(pages: List<CanvasPage>, sourceWidth: Float, sourceHeight: Float): List<CanvasPage> {
        if (sourceWidth <= 0f || sourceHeight <= 0f) return pages.ifEmpty { listOf(emptyList()) }
        val sx = CanvasPaper.WIDTH / sourceWidth
        val sy = CanvasPaper.HEIGHT / sourceHeight
        if (abs(sx - 1f) < 0.001f && abs(sy - 1f) < 0.001f) return pages.ifEmpty { listOf(emptyList()) }
        return pages.map { page ->
            page.map { element ->
                when (element) {
                    is StrokeElement -> element.copy(points = element.points.map { Offset(it.x * sx, it.y * sy) })
                    is ImageElement -> element.copy(
                        left = element.left * sx,
                        top = element.top * sy,
                        width = element.width * sx,
                        height = element.height * sy,
                        initialLeft = element.initialLeft * sx,
                        initialTop = element.initialTop * sy,
                        initialWidth = element.initialWidth * sx,
                        initialHeight = element.initialHeight * sy
                    )
                }
            }
        }.ifEmpty { listOf(emptyList()) }
    }

    private fun elementToJson(element: CanvasElement): JSONObject = when (element) {
        is StrokeElement -> JSONObject().apply {
            put("t", "s")
            put("id", element.id)
            put("c", element.colorArgb)
            put("w", element.baseWidth.toDouble())
            put("hl", if (element.highlighter) 1 else 0)
            put("pts", JSONArray().apply {
                element.points.forEach { point ->
                    put(round1(point.x))
                    put(round1(point.y))
                }
            })
            if (element.pressures.isNotEmpty()) {
                put("pr", JSONArray().apply { element.pressures.forEach { put(round2(it)) } })
            }
        }
        is ImageElement -> JSONObject().apply {
            put("t", "i")
            put("id", element.id)
            put("a", element.asset)
            put("x", round1(element.left))
            put("y", round1(element.top))
            put("w", round1(element.width))
            put("h", round1(element.height))
            put("r", round1(element.rotation))
            put("ox", round1(element.initialLeft))
            put("oy", round1(element.initialTop))
            put("ow", round1(element.initialWidth))
            put("oh", round1(element.initialHeight))
        }
    }

    private fun elementToModel(json: JSONObject?): CanvasElement? {
        if (json == null) return null
        return when (json.optString("t")) {
            "s" -> {
                val flat = json.optJSONArray("pts") ?: return null
                val points = ArrayList<Offset>(flat.length() / 2)
                var i = 0
                while (i + 1 < flat.length()) {
                    points.add(Offset(flat.optDouble(i).toFloat(), flat.optDouble(i + 1).toFloat()))
                    i += 2
                }
                if (points.isEmpty()) return null
                val pressuresJson = json.optJSONArray("pr")
                val pressures = if (pressuresJson != null) {
                    (0 until pressuresJson.length()).map { pressuresJson.optDouble(it, 1.0).toFloat() }
                } else emptyList()
                StrokeElement(
                    id = json.optString("id", UUID.randomUUID().toString()),
                    colorArgb = json.optInt("c", Color.Black.toArgb()),
                    baseWidth = json.optDouble("w", 3.0).toFloat(),
                    points = points,
                    pressures = pressures,
                    highlighter = json.optInt("hl", 0) == 1
                )
            }
            "i" -> {
                val asset = json.optString("a")
                if (asset.isBlank()) return null
                ImageElement(
                    id = json.optString("id", UUID.randomUUID().toString()),
                    asset = asset,
                    left = json.optDouble("x", 0.0).toFloat(),
                    top = json.optDouble("y", 0.0).toFloat(),
                    width = json.optDouble("w", 1.0).toFloat().coerceAtLeast(1f),
                    height = json.optDouble("h", 1.0).toFloat().coerceAtLeast(1f),
                    rotation = json.optDouble("r", 0.0).toFloat(),
                    initialLeft = json.optDouble("ox", json.optDouble("x", 0.0)).toFloat(),
                    initialTop = json.optDouble("oy", json.optDouble("y", 0.0)).toFloat(),
                    initialWidth = json.optDouble("ow", json.optDouble("w", 1.0)).toFloat().coerceAtLeast(1f),
                    initialHeight = json.optDouble("oh", json.optDouble("h", 1.0)).toFloat().coerceAtLeast(1f)
                )
            }
            else -> null
        }
    }

    /** Eski string biçimi: `colorName_width_x,y;x,y|colorName_width_...` */
    private fun parseLegacyPayload(data: String): CanvasPage {
        if (data.isBlank()) return emptyList()
        val elements = mutableListOf<CanvasElement>()
        runCatching {
            data.split("|").forEach { pathStr ->
                val parts = pathStr.split("_")
                if (parts.size >= 3) {
                    val colorArgb = when (parts[0]) {
                        "black" -> 0xFF000000.toInt()
                        "blue" -> 0xFF3B82F6.toInt()
                        "red" -> 0xFFEF4444.toInt()
                        "green" -> 0xFF10B981.toInt()
                        "amber" -> 0xFFF59E0B.toInt()
                        else -> 0xFF000000.toInt()
                    }
                    val width = parts[1].toFloatOrNull() ?: 5f
                    val points = parts[2].split(";").mapNotNull { ptStr ->
                        val coords = ptStr.split(",")
                        val x = coords.getOrNull(0)?.toFloatOrNull()
                        val y = coords.getOrNull(1)?.toFloatOrNull()
                        if (x != null && y != null) Offset(x, y) else null
                    }
                    if (points.isNotEmpty()) {
                        elements.add(StrokeElement(colorArgb = colorArgb, baseWidth = width, points = points))
                    }
                }
            }
        }
        return elements
    }

    private fun round1(value: Float): Double = (Math.round(value * 10.0) / 10.0)
    private fun round2(value: Float): Double = (Math.round(value * 100.0) / 100.0)
}

/** Bir elemanın canvas koordinatındaki sınır kutusu (stroke için çizgi kalınlığı dahil). */
fun elementBounds(element: CanvasElement): Rect = when (element) {
    is ImageElement -> element.rect
    is StrokeElement -> {
        if (element.points.isEmpty()) Rect.Zero else {
            var left = Float.MAX_VALUE
            var top = Float.MAX_VALUE
            var right = -Float.MAX_VALUE
            var bottom = -Float.MAX_VALUE
            element.points.forEach { p ->
                left = min(left, p.x); top = min(top, p.y)
                right = max(right, p.x); bottom = max(bottom, p.y)
            }
            val inset = element.baseWidth / 2f
            Rect(left - inset, top - inset, right + inset, bottom + inset)
        }
    }
}

/** Birden çok elemanın birleşik sınır kutusu. */
fun unionBounds(elements: List<CanvasElement>): Rect? {
    var result: Rect? = null
    elements.forEach { element ->
        val bounds = elementBounds(element)
        result = result?.let {
            Rect(
                min(it.left, bounds.left),
                min(it.top, bounds.top),
                max(it.right, bounds.right),
                max(it.bottom, bounds.bottom)
            )
        } ?: bounds
    }
    return result
}
