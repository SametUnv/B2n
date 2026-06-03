package com.board2notes.app.presentation.canvas

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Rotate90DegreesCcw
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class CanvasInputSettings(
    val allowFingerDrawing: Boolean,
    val useStylusPressure: Boolean,
    val palmRejection: Boolean,
    val minZoom: Float,
    val maxZoom: Float
)

private enum class SingleAction { DrawPen, Erase, Pan, SelectInteract, None }
private enum class SelectMode { Move, Resize, Crop, Marquee, None }
private enum class Corner { TL, TR, BL, BR }

private const val PAPER_WIDTH = CanvasPaper.WIDTH
private const val PAPER_HEIGHT = CanvasPaper.HEIGHT
private const val PAPER_VIEWPORT_PADDING = 18f
private const val PAPER_OVERSCROLL = 72f

private val PaperBounds = Rect(0f, 0f, PAPER_WIDTH, PAPER_HEIGHT)
private val PaperSize = Size(PAPER_WIDTH, PAPER_HEIGHT)
private val CanvasOutsideColor = Color(0xFFE2E5EA)
private val PaperBorderColor = Color(0xFFE2E8F0)

private fun screenToCanvas(point: Offset, offset: Offset, scale: Float): Offset =
    Offset((point.x - offset.x) / scale, (point.y - offset.y) / scale)

private fun canvasToScreen(point: Offset, offset: Offset, scale: Float): Offset =
    Offset(point.x * scale + offset.x, point.y * scale + offset.y)

private fun normalizedRect(a: Offset, b: Offset): Rect =
    Rect(min(a.x, b.x), min(a.y, b.y), max(a.x, b.x), max(a.y, b.y))

private fun pointOnPaper(point: Offset): Boolean = PaperBounds.contains(point)

private fun clampPointToPaper(point: Offset): Offset =
    Offset(point.x.coerceIn(0f, PAPER_WIDTH), point.y.coerceIn(0f, PAPER_HEIGHT))

private fun fitPaperScale(canvasSize: IntSize): Float {
    if (canvasSize.width <= 0 || canvasSize.height <= 0) return 1f
    val availableWidth = (canvasSize.width.toFloat() - PAPER_VIEWPORT_PADDING * 2f).coerceAtLeast(1f)
    return (availableWidth / PAPER_WIDTH).coerceAtLeast(0.1f)
}

private fun effectiveMinZoom(canvasSize: IntSize, input: CanvasInputSettings): Float {
    val fit = fitPaperScale(canvasSize)
    val configuredMin = input.minZoom.coerceAtLeast(0.1f)
    val limitedMin = max(configuredMin, fit * 0.82f)
    return min(limitedMin, fit).coerceAtMost(input.maxZoom.coerceAtLeast(0.1f))
}

private fun initialPaperScale(canvasSize: IntSize, input: CanvasInputSettings): Float {
    val minZoom = effectiveMinZoom(canvasSize, input)
    val maxZoom = input.maxZoom.coerceAtLeast(minZoom)
    return fitPaperScale(canvasSize).coerceIn(minZoom, maxZoom)
}

private fun centeredPaperOffset(canvasSize: IntSize, scale: Float): Offset {
    val paperWidth = PAPER_WIDTH * scale
    val paperHeight = PAPER_HEIGHT * scale
    val viewportWidth = canvasSize.width.toFloat()
    val viewportHeight = canvasSize.height.toFloat()
    return Offset(
        x = if (paperWidth <= viewportWidth) (viewportWidth - paperWidth) / 2f else 0f,
        y = if (paperHeight <= viewportHeight) (viewportHeight - paperHeight) / 2f else PAPER_VIEWPORT_PADDING
    )
}

private fun clampViewportOffset(offset: Offset, scale: Float, canvasSize: IntSize): Offset {
    if (canvasSize.width <= 0 || canvasSize.height <= 0) return offset
    val viewportWidth = canvasSize.width.toFloat()
    val viewportHeight = canvasSize.height.toFloat()
    val paperWidth = PAPER_WIDTH * scale
    val paperHeight = PAPER_HEIGHT * scale

    fun clampAxis(value: Float, viewport: Float, content: Float): Float {
        if (content <= viewport) return (viewport - content) / 2f
        val minOffset = viewport - content - PAPER_OVERSCROLL
        val maxOffset = PAPER_OVERSCROLL
        return value.coerceIn(minOffset, maxOffset)
    }

    return Offset(
        x = clampAxis(offset.x, viewportWidth, paperWidth),
        y = clampAxis(offset.y, viewportHeight, paperHeight)
    )
}

/**
 * Profesyonel çizim yüzeyi: kalem/parmak/basınç/avuç reddi (ayarlara bağlı), pan/zoom,
 * çizgi + görsel render, seçim kutusu + köşe boyutlandırma tutamakları, dock'lanan araç çubuğu.
 */
@Composable
fun CanvasEditor(
    controller: CanvasController,
    filesDir: File,
    noteId: String,
    input: CanvasInputSettings,
    recentColors: List<Color>,
    onColorUsed: (Color) -> Unit,
    onAddImage: () -> Unit,
    onConvertImageToText: (ImageElement) -> Unit = {},
    convertingAssets: Set<String> = emptySet(),
    pageControlsVisible: Boolean = true,
    onChromeVisibleChange: (Boolean) -> Unit = {},
    isFullscreen: Boolean = false,
    onFullscreenChange: (Boolean) -> Unit = {},
    pendingInsertAsset: String? = null,
    pendingInsertPageIndex: Int? = null,
    pendingInsertJobId: String? = null,
    onPendingInsertConsumed: () -> Unit = {},
    allowLegacyBackgroundFallback: Boolean = false,
    useDarkTheme: Boolean = false,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var lastFittedPage by remember(noteId) { mutableStateOf<Int?>(null) }

    // Geçici (canlı) çizim durumu — pointer döngüsü tarafından güncellenir, render tarafından okunur.
    var liveStrokePoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var liveStrokePressures by remember { mutableStateOf<List<Float>>(emptyList()) }
    var eraserCursor by remember { mutableStateOf<Offset?>(null) }
    var marqueeRect by remember { mutableStateOf<Rect?>(null) }

    var showColorMenu by remember { mutableStateOf(false) }
    var showWidthMenu by remember { mutableStateOf(false) }
    var showEraserMenu by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    fun hideChromeForGesture() {
        onChromeVisibleChange(false)
    }

    fun showChromeFromGesture() {
        if (!isFullscreen) onChromeVisibleChange(true)
    }

    LaunchedEffect(canvasSize, controller.safePageIndex, input.minZoom, input.maxZoom) {
        if (canvasSize.width <= 0 || canvasSize.height <= 0) return@LaunchedEffect
        val minZoom = effectiveMinZoom(canvasSize, input)
        val maxZoom = input.maxZoom.coerceAtLeast(minZoom)
        if (lastFittedPage != controller.safePageIndex) {
            val scale = initialPaperScale(canvasSize, input)
            controller.viewportScale = scale
            controller.viewportOffset = clampViewportOffset(centeredPaperOffset(canvasSize, scale), scale, canvasSize)
            lastFittedPage = controller.safePageIndex
        } else {
            val scale = controller.viewportScale.coerceIn(minZoom, maxZoom)
            controller.viewportScale = scale
            controller.viewportOffset = clampViewportOffset(controller.viewportOffset, scale, canvasSize)
        }
    }

    // Görsel asset bitmap önbelleği
    val bitmapCache = remember(noteId) { mutableStateMapOf<String, ImageBitmap>() }
    LaunchedEffect(noteId, controller.pages, controller.safePageIndex) {
        val assets = controller.currentPage().filterIsInstance<ImageElement>().map { it.asset }.toSet()
        withContext(Dispatchers.IO) {
            assets.forEach { asset ->
                if (!bitmapCache.containsKey(asset)) {
                    val file = CanvasAssets.imageFile(filesDir, noteId, asset)
                    val bmp = if (file.exists()) BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() else null
                    if (bmp != null) bitmapCache[asset] = bmp
                }
            }
        }
    }

    // Düzenleme arka planı önizlemeden ayrıdır. Eski belgelerde canvas.png bir kez fallback olarak okunur.
    var background by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(noteId, controller.safePageIndex, allowLegacyBackgroundFallback) {
        background = withContext(Dispatchers.IO) {
            val dedicated = CanvasAssets.backgroundFile(filesDir, noteId, controller.safePageIndex)
            val legacy = CanvasAssets.previewFile(filesDir, noteId, controller.safePageIndex)
            val file = dedicated.takeIf { it.exists() }
                ?: legacy.takeIf { allowLegacyBackgroundFallback && it.exists() }
            file?.let { BitmapFactory.decodeFile(it.absolutePath)?.asImageBitmap() }
        }
    }

    // Tahta çıktısı / galeri görseli: merkezi ve seçili olarak yerleştir (canvasSize bilindiğinde).
    LaunchedEffect(pendingInsertAsset, pendingInsertPageIndex, pendingInsertJobId, canvasSize, controller.safePageIndex) {
        val asset = pendingInsertAsset
        if (asset != null && canvasSize.width > 0 && canvasSize.height > 0) {
            val targetPageIndex = pendingInsertPageIndex?.coerceIn(0, controller.pageCount - 1)
            if (targetPageIndex != null && targetPageIndex != controller.safePageIndex) {
                controller.goToPage(targetPageIndex)
                return@LaunchedEffect
            }
            val file = CanvasAssets.imageFile(filesDir, noteId, asset)
            val bmp = withContext(Dispatchers.IO) {
                if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
            }
            if (bmp != null) {
                val sc = controller.viewportScale
                val maxW = PAPER_WIDTH * 0.86f
                val maxH = PAPER_HEIGHT * 0.72f
                val ratio = bmp.height.toFloat() / bmp.width.toFloat().coerceAtLeast(1f)
                var w = min(maxW, bmp.width.toFloat())
                var h = w * ratio
                if (h > maxH) { h = maxH; w = h / ratio.coerceAtLeast(0.0001f) }
                val visibleCenter = screenToCanvas(
                    Offset(canvasSize.width / 2f, canvasSize.height / 2f),
                    controller.viewportOffset, sc
                )
                val center = clampPointToPaper(visibleCenter)
                val left = (center.x - w / 2f).coerceIn(0f, (PAPER_WIDTH - w).coerceAtLeast(0f))
                val top = (center.y - h / 2f).coerceIn(0f, (PAPER_HEIGHT - h).coerceAtLeast(0f))
                bitmapCache[asset] = bmp.asImageBitmap()
                controller.addElement(
                    ImageElement(
                        asset = asset,
                        left = left,
                        top = top,
                        width = w,
                        height = h,
                        backendJobId = pendingInsertJobId
                    ),
                    select = true
                )
                controller.tool = CanvasTool.Select
            }
            onPendingInsertConsumed()
        }
    }

    fun pressureOf(type: PointerType, pressure: Float): Float =
        if (input.useStylusPressure && type == PointerType.Stylus) pressure.coerceIn(0.08f, 1f) else 1f

    fun decideAction(type: PointerType): SingleAction = when (controller.tool) {
        CanvasTool.Select -> SingleAction.SelectInteract
        CanvasTool.Pen -> if (type == PointerType.Stylus || type == PointerType.Mouse || input.allowFingerDrawing) SingleAction.DrawPen else SingleAction.Pan
        CanvasTool.Eraser -> if (type == PointerType.Stylus || type == PointerType.Mouse || input.allowFingerDrawing) SingleAction.Erase else SingleAction.Pan
        CanvasTool.Pan -> SingleAction.Pan
    }

    fun handleScreenCorners(bounds: Rect): Map<Corner, Offset> {
        val off = controller.viewportOffset; val sc = controller.viewportScale
        return mapOf(
            Corner.TL to canvasToScreen(Offset(bounds.left, bounds.top), off, sc),
            Corner.TR to canvasToScreen(Offset(bounds.right, bounds.top), off, sc),
            Corner.BL to canvasToScreen(Offset(bounds.left, bounds.bottom), off, sc),
            Corner.BR to canvasToScreen(Offset(bounds.right, bounds.bottom), off, sc)
        )
    }

    fun handleScreenEdges(bounds: Rect): Map<CropEdge, Offset> {
        val off = controller.viewportOffset; val sc = controller.viewportScale
        return mapOf(
            CropEdge.Left to canvasToScreen(Offset(bounds.left, bounds.center.y), off, sc),
            CropEdge.Top to canvasToScreen(Offset(bounds.center.x, bounds.top), off, sc),
            CropEdge.Right to canvasToScreen(Offset(bounds.right, bounds.center.y), off, sc),
            CropEdge.Bottom to canvasToScreen(Offset(bounds.center.x, bounds.bottom), off, sc)
        )
    }

    val invertMatrix = remember {
        androidx.compose.ui.graphics.ColorMatrix(floatArrayOf(
            -1f,  0f,  0f, 0f, 255f,
             0f, -1f,  0f, 0f, 255f,
             0f,  0f, -1f, 0f, 255f,
             0f,  0f,  0f, 1f,   0f
        ))
    }
    val invertFilter = remember(invertMatrix) { androidx.compose.ui.graphics.ColorFilter.colorMatrix(invertMatrix) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasOutsideColor)
            .onSizeChanged { canvasSize = it }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .pointerInput(controller.tool, input, controller.safePageIndex, canvasSize) {
                    awaitEachGesture {
                        val firstEvent = awaitPointerEvent()
                        val firstDown = firstEvent.changes.firstOrNull { it.pressed } ?: return@awaitEachGesture
                        // Araç çubuğu/üst katman bu dokunuşu tükettiyse canvas'a çizme.
                        if (firstDown.isConsumed) return@awaitEachGesture

                        var pinching = false
                        var prevCentroid: Offset? = null
                        var prevDist = 0f

                        var action = SingleAction.None
                        var selectMode = SelectMode.None
                        var resizeCorner = Corner.BR
                        var cropEdge = CropEdge.Right
                        var resizeAnchor = Offset.Zero
                        var startBounds: Rect? = null
                        var sessionStarted = false
                        var erasing = false
                        var totalDrag = 0f
                        val startCanvas = screenToCanvas(firstDown.position, controller.viewportOffset, controller.viewportScale)

                        // İlk dokunuş aksiyonunu belirle
                        action = decideAction(firstDown.type)
                        if (!pointOnPaper(startCanvas) && action != SingleAction.Pan) {
                            action = SingleAction.Pan
                        }
                        when (action) {
                            SingleAction.DrawPen -> {
                                liveStrokePoints = listOf(startCanvas)
                                liveStrokePressures = listOf(pressureOf(firstDown.type, firstDown.pressure))
                            }
                            SingleAction.Erase -> { eraserCursor = startCanvas }
                            SingleAction.SelectInteract -> {
                                val bounds = controller.selectionBounds()
                                if (bounds != null) {
                                    val corners = handleScreenCorners(bounds)
                                    val hit = corners.minByOrNull { (firstDown.position - it.value).getDistance() }
                                    if (hit != null && (firstDown.position - hit.value).getDistance() <= 36f) {
                                        selectMode = SelectMode.Resize
                                        resizeCorner = hit.key
                                        startBounds = bounds
                                        resizeAnchor = when (hit.key) {
                                            Corner.TL -> Offset(bounds.right, bounds.bottom)
                                            Corner.TR -> Offset(bounds.left, bounds.bottom)
                                            Corner.BL -> Offset(bounds.right, bounds.top)
                                            Corner.BR -> Offset(bounds.left, bounds.top)
                                        }
                                    } else {
                                        val selectedHasImage = controller.selectedElements().any { it is ImageElement }
                                        val edgeHit = if (selectedHasImage) {
                                            handleScreenEdges(bounds).minByOrNull { (firstDown.position - it.value).getDistance() }
                                        } else {
                                            null
                                        }
                                        if (edgeHit != null && (firstDown.position - edgeHit.value).getDistance() <= 34f) {
                                            selectMode = SelectMode.Crop
                                            cropEdge = edgeHit.key
                                            startBounds = bounds
                                        } else {
                                            val off = controller.viewportOffset; val sc = controller.viewportScale
                                            val screenRect = Rect(
                                                canvasToScreen(Offset(bounds.left, bounds.top), off, sc),
                                                canvasToScreen(Offset(bounds.right, bounds.bottom), off, sc)
                                            )
                                            selectMode = if (screenRect.contains(firstDown.position)) SelectMode.Move else SelectMode.Marquee
                                        }
                                    }
                                } else {
                                    selectMode = SelectMode.Marquee
                                }
                            }
                            else -> {}
                        }
                        firstDown.consume()

                        while (true) {
                            val event = awaitPointerEvent()
                            var pressed = event.changes.filter { it.pressed }
                            // Avuç içi reddi: kalem aktifken parmak temaslarını yok say
                            if (input.palmRejection && pressed.any { it.type == PointerType.Stylus }) {
                                pressed = pressed.filter { it.type == PointerType.Stylus }
                            }

                            if (pressed.size >= 2) {
                                if (!pinching) {
                                    hideChromeForGesture()
                                    pinching = true
                                    liveStrokePoints = emptyList(); liveStrokePressures = emptyList()
                                    eraserCursor = null; marqueeRect = null
                                    if (erasing) { controller.endErase(); erasing = false }
                                    if (sessionStarted) {
                                        if (selectMode == SelectMode.Crop) controller.endCropSession() else controller.endTransformSession()
                                        sessionStarted = false
                                    }
                                    prevCentroid = null; prevDist = 0f
                                }
                                val p0 = pressed[0].position; val p1 = pressed[1].position
                                val centroid = Offset((p0.x + p1.x) / 2f, (p0.y + p1.y) / 2f)
                                val dist = (p0 - p1).getDistance().coerceAtLeast(1f)
                                val pc = prevCentroid
                                if (pc != null && prevDist > 0f) {
                                    val zoom = (dist / prevDist).coerceIn(0.8f, 1.25f)
                                    val oldScale = controller.viewportScale
                                    val minZoom = effectiveMinZoom(canvasSize, input)
                                    val maxZoom = input.maxZoom.coerceAtLeast(minZoom)
                                    var newScale = (oldScale * zoom).coerceIn(minZoom, maxZoom)
                                    if (newScale.isNaN() || newScale.isInfinite()) {
                                        newScale = oldScale
                                    }
                                    // Focus should be the canvas coordinate of the previous centroid before scale update
                                    val focus = screenToCanvas(pc, controller.viewportOffset, oldScale)
                                    // Calculate new offset such that the canvas focus coordinate aligns with the new centroid
                                    var newOffset = Offset(
                                        centroid.x - focus.x * newScale,
                                        centroid.y - focus.y * newScale
                                    )
                                    if (newOffset.x.isNaN() || newOffset.x.isInfinite() || newOffset.y.isNaN() || newOffset.y.isInfinite()) {
                                        newOffset = controller.viewportOffset
                                    }
                                    controller.viewportScale = newScale
                                    controller.viewportOffset = clampViewportOffset(newOffset, newScale, canvasSize)
                                }
                                prevCentroid = centroid; prevDist = dist
                                event.changes.forEach { it.consume() }
                            } else if (pressed.size == 1 && !pinching) {
                                val ch = pressed[0]
                                val canvasPt = screenToCanvas(ch.position, controller.viewportOffset, controller.viewportScale)
                                val delta = Offset(ch.position.x - ch.previousPosition.x, ch.position.y - ch.previousPosition.y)
                                totalDrag += abs(delta.x) + abs(delta.y)
                                when (action) {
                                    SingleAction.DrawPen -> {
                                        if (pointOnPaper(canvasPt)) {
                                            liveStrokePoints = liveStrokePoints + canvasPt
                                            liveStrokePressures = liveStrokePressures + pressureOf(ch.type, ch.pressure)
                                        }
                                    }
                                    SingleAction.Erase -> {
                                        if (!erasing) { controller.beginErase(); erasing = true }
                                        if (pointOnPaper(canvasPt)) {
                                            eraserCursor = canvasPt
                                            controller.eraseStrokesAt(canvasPt, controller.eraserSize)
                                        } else {
                                            eraserCursor = null
                                        }
                                    }
                                    SingleAction.Pan -> {
                                        if (totalDrag > 18f) {
                                            when {
                                                delta.y < -1.5f -> hideChromeForGesture()
                                                delta.y > 1.5f -> showChromeFromGesture()
                                            }
                                        }
                                        controller.viewportOffset = clampViewportOffset(
                                            controller.viewportOffset + delta,
                                            controller.viewportScale,
                                            canvasSize
                                        )
                                    }
                                    SingleAction.SelectInteract -> when (selectMode) {
                                        SelectMode.Move -> {
                                            if (!sessionStarted && totalDrag > 6f && controller.selection.isNotEmpty()) {
                                                controller.beginTransformSession(); sessionStarted = true
                                            }
                                            if (sessionStarted) {
                                                controller.sessionTranslate(Offset(canvasPt.x - startCanvas.x, canvasPt.y - startCanvas.y))
                                            }
                                        }
                                        SelectMode.Resize -> {
                                            val b = startBounds
                                            if (b != null && b.width > 1f && b.height > 1f) {
                                                if (!sessionStarted && totalDrag > 4f) { controller.beginTransformSession(); sessionStarted = true }
                                                if (sessionStarted) {
                                                    val sx = (abs(canvasPt.x - resizeAnchor.x) / b.width).coerceIn(0.05f, 20f)
                                                    val sy = (abs(canvasPt.y - resizeAnchor.y) / b.height).coerceIn(0.05f, 20f)
                                                    controller.sessionScale(resizeAnchor, sx, sy)
                                                }
                                            }
                                        }
                                        SelectMode.Crop -> {
                                            if (startBounds != null) {
                                                if (!sessionStarted && totalDrag > 4f) {
                                                    controller.beginCropSession()
                                                    sessionStarted = true
                                                }
                                                if (sessionStarted) {
                                                    controller.sessionCropSelectedImages(
                                                        cropEdge,
                                                        Offset(canvasPt.x - startCanvas.x, canvasPt.y - startCanvas.y)
                                                    )
                                                }
                                            }
                                        }
                                        SelectMode.Marquee -> { marqueeRect = normalizedRect(startCanvas, clampPointToPaper(canvasPt)) }
                                        SelectMode.None -> {}
                                    }
                                    SingleAction.None -> {}
                                }
                                ch.consume()
                            }

                            if (event.changes.all { !it.pressed }) break
                        }

                        // Jest sonu
                        if (pinching) {
                            liveStrokePoints = emptyList(); liveStrokePressures = emptyList()
                            eraserCursor = null; marqueeRect = null
                        } else when (action) {
                            SingleAction.DrawPen -> {
                                if (liveStrokePoints.isNotEmpty()) {
                                    controller.addElement(
                                        StrokeElement(
                                            colorArgb = controller.color.toArgb(),
                                            baseWidth = controller.strokeWidth,
                                            points = liveStrokePoints,
                                            pressures = liveStrokePressures
                                        )
                                    )
                                }
                                liveStrokePoints = emptyList(); liveStrokePressures = emptyList()
                            }
                            SingleAction.Erase -> {
                                if (erasing) { controller.endErase(); erasing = false }
                                eraserCursor = null
                            }
                            SingleAction.Pan -> Unit
                            SingleAction.SelectInteract -> when (selectMode) {
                                SelectMode.Move, SelectMode.Resize -> {
                                    if (sessionStarted) { controller.endTransformSession(); sessionStarted = false }
                                    else controller.selectAt(startCanvas)
                                }
                                SelectMode.Crop -> {
                                    if (sessionStarted) { controller.endCropSession(); sessionStarted = false }
                                    else controller.selectAt(startCanvas)
                                }
                                SelectMode.Marquee -> {
                                    val r = marqueeRect
                                    if (totalDrag > 8f && r != null) controller.selectInRect(r) else controller.selectAt(startCanvas)
                                    marqueeRect = null
                                }
                                SelectMode.None -> {}
                            }
                            else -> {}
                        }
                    }
                }
        ) {
            val off = controller.viewportOffset
            val sc = controller.viewportScale
            drawRect(CanvasOutsideColor, size = size)
            withTransform({ translate(off.x, off.y); scale(sc, sc, pivot = Offset.Zero) }) {
                drawRect(Color.White, topLeft = Offset.Zero, size = PaperSize)
                clipRect(left = 0f, top = 0f, right = PAPER_WIDTH, bottom = PAPER_HEIGHT) {
                    background?.let { bg ->
                        drawImage(
                            image = bg,
                            dstSize = IntSize(PAPER_WIDTH.roundToInt(), PAPER_HEIGHT.roundToInt())
                        )
                    }
                    controller.currentPage().forEach { element ->
                        when (element) {
                            is StrokeElement -> drawStrokeElement(element, useDarkTheme)
                            is ImageElement -> drawImageElement(element, bitmapCache[element.asset])
                        }
                    }
                    if (liveStrokePoints.isNotEmpty()) {
                        drawRawStroke(liveStrokePoints, liveStrokePressures, controller.color, controller.strokeWidth, useDarkTheme)
                    }
                    eraserCursor?.let { c ->
                        drawCircle(Color(0x33EF4444), radius = controller.eraserSize, center = c)
                        drawCircle(Color(0xFFEF4444), radius = controller.eraserSize, center = c, style = Stroke(width = 1.5f / sc))
                    }
                    marqueeRect?.let { r ->
                        drawRect(
                            color = primary,
                            topLeft = Offset(r.left, r.top),
                            size = Size(r.width, r.height),
                            style = Stroke(width = 1.8f / sc, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)))
                        )
                    }
                }
                drawRect(
                    color = PaperBorderColor,
                    topLeft = Offset.Zero,
                    size = PaperSize,
                    style = Stroke(width = 1.25f / sc)
                )
            }

            // Seçim kutusu + tutamaklar (ekran uzayında sabit boyutlu)
            val selBounds = controller.selectionBounds()
            if (selBounds != null && controller.tool == CanvasTool.Select) {
                val tl = canvasToScreen(Offset(selBounds.left, selBounds.top), off, sc)
                val br = canvasToScreen(Offset(selBounds.right, selBounds.bottom), off, sc)
                val selectedHasImage = controller.selectedElements().any { it is ImageElement }
                drawRect(
                    color = primary,
                    topLeft = tl,
                    size = Size(br.x - tl.x, br.y - tl.y),
                    style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 8f)))
                )
                listOf(tl, Offset(br.x, tl.y), Offset(tl.x, br.y), br).forEach { corner ->
                    drawCircle(Color.White, radius = 13f, center = corner)
                    drawCircle(primary, radius = 13f, center = corner, style = Stroke(width = 3f))
                }
                if (selectedHasImage) {
                    val edgeHandles = listOf(
                        Offset((tl.x + br.x) / 2f, tl.y),
                        Offset(br.x, (tl.y + br.y) / 2f),
                        Offset((tl.x + br.x) / 2f, br.y),
                        Offset(tl.x, (tl.y + br.y) / 2f)
                    )
                    edgeHandles.forEach { handle ->
                        drawRoundRect(
                            color = Color.White,
                            topLeft = Offset(handle.x - 10f, handle.y - 6f),
                            size = Size(20f, 12f),
                            cornerRadius = CornerRadius(4f, 4f)
                        )
                        drawRoundRect(
                            color = primary,
                            topLeft = Offset(handle.x - 10f, handle.y - 6f),
                            size = Size(20f, 12f),
                            cornerRadius = CornerRadius(4f, 4f),
                            style = Stroke(width = 2.4f)
                        )
                    }
                }
            }
        }

        // Dock'lanan araç çubuğu
        val selectedBounds = controller.selectionBounds()
        if (selectedBounds != null && controller.tool == CanvasTool.Select && canvasSize.width > 0) {
            val off = controller.viewportOffset
            val sc = controller.viewportScale
            val tl = canvasToScreen(Offset(selectedBounds.left, selectedBounds.top), off, sc)
            val br = canvasToScreen(Offset(selectedBounds.right, selectedBounds.bottom), off, sc)
            val canRestoreImage = controller.selectedElements().any { it is ImageElement }
            val singleImage = controller.selectedElements().filterIsInstance<ImageElement>().singleOrNull()
            val converting = singleImage != null && singleImage.asset in convertingAssets
            SelectionActionBubble(
                canRestore = canRestoreImage,
                canCrop = canRestoreImage,
                canConvert = singleImage != null,
                converting = converting,
                hasOcr = singleImage?.ocrText?.isNotBlank() == true,
                onDelete = { controller.deleteSelection() },
                onRestore = { controller.restoreSelectedImagesToOriginalPlacement() },
                onCrop = { controller.tool = CanvasTool.Select },
                onRotate = { controller.rotateSelectedImages(90f) },
                onConvert = { singleImage?.let(onConvertImageToText) },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset {
                        val bubbleWidth = when {
                            singleImage != null -> 360
                            canRestoreImage -> 244
                            else -> 82
                        }
                        val x = ((tl.x + br.x) / 2f - bubbleWidth / 2f)
                            .roundToInt()
                            .coerceIn(8, (canvasSize.width - bubbleWidth - 8).coerceAtLeast(8))
                        val y = (tl.y - 70f)
                            .roundToInt()
                            .coerceIn(8, (canvasSize.height - 48).coerceAtLeast(8))
                        IntOffset(x, y)
                    }
            )
        }

        if (canvasSize.width > 0) {
            DockableToolbar(
                containerWidthPx = canvasSize.width.toFloat(),
                containerHeightPx = canvasSize.height.toFloat()
            ) { _ ->
                CanvasToolIconButton(Icons.Default.Brush, selected = controller.tool == CanvasTool.Pen) {
                    controller.tool = CanvasTool.Pen; controller.clearSelection()
                }
                Box {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .padding(5.dp)
                            .clip(CircleShape)
                            .background(controller.color)
                            .border(2.dp, Color(0xFFCBD5E1), CircleShape)
                            .clickable { showColorMenu = true }
                    )
                    DropdownMenu(expanded = showColorMenu, onDismissRequest = {
                        showColorMenu = false; onColorUsed(controller.color)
                    }) {
                        CanvasColorPicker(
                            initial = controller.color,
                            recents = recentColors,
                            onColor = { controller.color = it }
                        )
                    }
                }
                Box {
                    CanvasToolIconButton(Icons.Default.Tune, selected = showWidthMenu) { showWidthMenu = true }
                    DropdownMenu(expanded = showWidthMenu, onDismissRequest = { showWidthMenu = false }) {
                        Column(Modifier.width(220.dp).padding(12.dp)) {
                            Text("Kalem kalınlığı: ${controller.strokeWidth.roundToInt()}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            Slider(value = controller.strokeWidth, onValueChange = { controller.strokeWidth = it }, valueRange = 1f..100f)
                            Canvas(Modifier.width(196.dp).height(28.dp)) {
                                drawLine(controller.color, Offset(8f, size.height / 2f), Offset(size.width - 8f, size.height / 2f), strokeWidth = controller.strokeWidth, cap = StrokeCap.Round)
                            }
                        }
                    }
                }
                Box {
                    CanvasToolIconButton(eraserIcon(), selected = controller.tool == CanvasTool.Eraser) {
                        controller.tool = CanvasTool.Eraser; controller.clearSelection(); showEraserMenu = true
                    }
                    DropdownMenu(expanded = showEraserMenu, onDismissRequest = { showEraserMenu = false }) {
                        Column(Modifier.width(220.dp).padding(12.dp)) {
                            Text("Silgi boyutu: ${controller.eraserSize.roundToInt()}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            Slider(value = controller.eraserSize, onValueChange = { controller.eraserSize = it }, valueRange = 12f..90f)
                        }
                    }
                }
                CanvasToolIconButton(Icons.Default.CropFree, selected = controller.tool == CanvasTool.Select) {
                    controller.tool = CanvasTool.Select
                }
                CanvasToolIconButton(Icons.Default.PanTool, selected = controller.tool == CanvasTool.Pan) {
                    controller.tool = CanvasTool.Pan; controller.clearSelection()
                }
                CanvasToolIconButton(Icons.Default.Undo, selected = false, enabled = controller.canUndo) { controller.undo() }
                CanvasToolIconButton(Icons.Default.Redo, selected = false, enabled = controller.canRedo) { controller.redo() }
                CanvasToolIconButton(Icons.Default.AddPhotoAlternate, selected = false) { onAddImage() }
                CanvasToolIconButton(Icons.Default.Add, selected = false) { controller.addPage() }
                CanvasToolIconButton(Icons.Default.Delete, selected = false) {
                    if (controller.selection.isNotEmpty()) controller.deleteSelection() else showClearDialog = true
                }
            }
        }

        // Sayfa gezinme çubuğu
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 12.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)),
            shadowElevation = 6.dp
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clickable {
                        val next = !isFullscreen
                        onFullscreenChange(next)
                        onChromeVisibleChange(!next)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    contentDescription = if (isFullscreen) "Tam ekrandan cik" else "Tam ekran",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = pageControlsVisible,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp),
            enter = fadeIn(tween(140)) + scaleIn(tween(170), initialScale = 0.94f),
            exit = fadeOut(tween(110)) + scaleOut(tween(130), targetScale = 0.96f)
        ) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = 4.dp
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                TextButton(onClick = { controller.goToPage(controller.safePageIndex - 1) }, enabled = controller.safePageIndex > 0) {
                    Text("‹", color = if (controller.safePageIndex > 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f))
                }
                Text("Sayfa ${controller.safePageIndex + 1} / ${controller.pageCount}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { controller.goToPage(controller.safePageIndex + 1) }, enabled = controller.safePageIndex < controller.pageCount - 1) {
                    Text("›", color = if (controller.safePageIndex < controller.pageCount - 1) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f))
                }
            }
        }
    }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Sayfayı temizle") },
            text = { Text("Bu sayfadaki tüm çizimler ve görseller silinecek. Diğer sayfalar etkilenmez.") },
            confirmButton = { Button(onClick = { controller.clearCurrentPage(); showClearDialog = false }) { Text("Temizle") } },
            dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("Vazgeç") } }
        )
    }
}

// --- Render yardımcıları ---

private fun mapStrokeColor(color: Color, useDarkTheme: Boolean): Color {
    if (!useDarkTheme) return color
    val r = color.red
    val g = color.green
    val b = color.blue
    if (r < 0.15f && g < 0.15f && b < 0.15f && color.alpha > 0.5f) {
        return Color.White
    }
    if (r > 0.85f && g > 0.85f && b > 0.85f && color.alpha > 0.5f) {
        return Color.Black
    }
    return color
}

private fun DrawScope.drawStrokeElement(stroke: StrokeElement, useDarkTheme: Boolean) {
    drawPolyline(stroke.points, stroke.pressures, mapStrokeColor(stroke.color, useDarkTheme), stroke.baseWidth, stroke.highlighter)
}

private fun DrawScope.drawRawStroke(points: List<Offset>, pressures: List<Float>, color: Color, baseWidth: Float, useDarkTheme: Boolean) {
    drawPolyline(points, pressures, mapStrokeColor(color, useDarkTheme), baseWidth, highlighter = false)
}

private fun DrawScope.drawPolyline(points: List<Offset>, pressures: List<Float>, color: Color, baseWidth: Float, highlighter: Boolean) {
    if (points.isEmpty()) return
    val drawColor = if (highlighter) color.copy(alpha = 0.4f) else color
    if (points.size == 1) {
        drawCircle(drawColor, radius = baseWidth / 2f, center = points.first())
        return
    }
    val uniform = highlighter || pressures.isEmpty() || pressures.all { abs(it - 1f) < 0.03f }
    if (uniform) {
        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
        }
        drawPath(path, drawColor, style = Stroke(width = baseWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
    } else {
        for (i in 1 until points.size) {
            val p0 = pressures.getOrElse(i - 1) { 1f }
            val p1 = pressures.getOrElse(i) { 1f }
            val w = (baseWidth * (p0 + p1) / 2f).coerceAtLeast(0.6f)
            drawLine(drawColor, points[i - 1], points[i], strokeWidth = w, cap = StrokeCap.Round)
        }
    }
}

private fun DrawScope.drawImageElement(element: ImageElement, bitmap: ImageBitmap?) {
    if (bitmap == null) {
        drawRect(Color(0x11000000), topLeft = Offset(element.left, element.top), size = Size(element.width, element.height))
        return
    }
    val cropLeft = element.cropLeft.coerceIn(0f, 0.95f)
    val cropTop = element.cropTop.coerceIn(0f, 0.95f)
    val cropRight = element.cropRight.coerceIn(0f, 0.95f - cropLeft)
    val cropBottom = element.cropBottom.coerceIn(0f, 0.95f - cropTop)
    val srcLeft = (bitmap.width * cropLeft).roundToInt().coerceIn(0, (bitmap.width - 1).coerceAtLeast(0))
    val srcTop = (bitmap.height * cropTop).roundToInt().coerceIn(0, (bitmap.height - 1).coerceAtLeast(0))
    val srcRight = (bitmap.width * (1f - cropRight)).roundToInt().coerceIn(srcLeft + 1, bitmap.width)
    val srcBottom = (bitmap.height * (1f - cropBottom)).roundToInt().coerceIn(srcTop + 1, bitmap.height)
    val draw: DrawScope.() -> Unit = {
        drawImage(
            image = bitmap,
            srcOffset = IntOffset(srcLeft, srcTop),
            srcSize = IntSize((srcRight - srcLeft).coerceAtLeast(1), (srcBottom - srcTop).coerceAtLeast(1)),
            dstOffset = IntOffset(element.left.roundToInt(), element.top.roundToInt()),
            dstSize = IntSize(element.width.roundToInt().coerceAtLeast(1), element.height.roundToInt().coerceAtLeast(1))
        )
    }
    if (element.rotation != 0f) rotate(element.rotation, pivot = element.center) { draw() } else draw()
}

// --- Araç çubuğu düğmesi ---

@Composable
private fun SelectionActionBubble(
    canRestore: Boolean,
    canCrop: Boolean,
    canConvert: Boolean,
    converting: Boolean,
    hasOcr: Boolean,
    onDelete: () -> Unit,
    onRestore: () -> Unit,
    onCrop: () -> Unit,
    onRotate: () -> Unit,
    onConvert: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Surface(
                modifier = Modifier.clickable(onClick = onDelete),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Sil",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            if (canConvert) {
                Surface(
                    modifier = if (converting) Modifier else Modifier.clickable(onClick = onConvert),
                    shape = RoundedCornerShape(18.dp),
                    color = if (hasOcr) MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        if (converting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(15.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                Icons.Default.TextFields,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Text(
                            text = when {
                                converting -> "Çevriliyor..."
                                hasOcr -> "Metni güncelle"
                                else -> "Metne çevir"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            if (canCrop) {
                Surface(
                    modifier = Modifier.clickable(onClick = onCrop),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
                ) {
                    Icon(
                        Icons.Default.CropFree,
                        contentDescription = "Kirp",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(8.dp).size(17.dp)
                    )
                }
                Surface(
                    modifier = Modifier.clickable(onClick = onRotate),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
                ) {
                    Icon(
                        Icons.Default.Rotate90DegreesCcw,
                        contentDescription = "Dondur",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(8.dp).size(17.dp)
                    )
                }
            }
            if (canRestore) {
                Surface(
                    modifier = Modifier.clickable(onClick = onRestore),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
                ) {
                    Icon(
                        Icons.Default.Restore,
                        contentDescription = "Ilk boyuta dondur",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(8.dp).size(17.dp)
                    )
                }
            }
        }
        }
    }
@Composable
private fun CanvasToolIconButton(
    icon: ImageVector,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val bg = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else Color.Transparent
    val tint = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
    }
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
    }
}

private var cachedEraserIcon: ImageVector? = null

private fun eraserIcon(): ImageVector {
    cachedEraserIcon?.let { return it }
    val built = ImageVector.Builder(name = "Eraser", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
        path(fill = null, stroke = SolidColor(Color(0xFF334155)), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(16.24f, 7.76f)
            lineTo(19.07f, 10.59f)
            lineTo(11.29f, 18.36f)
            lineTo(5.64f, 18.36f)
            lineTo(5.64f, 12.71f)
            close()
            moveTo(9.88f, 16.95f)
            lineTo(15.54f, 11.3f)
        }
    }.build()
    cachedEraserIcon = built
    return built
}
