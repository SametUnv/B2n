package com.board2notes.app.presentation.canvas

import android.graphics.BitmapFactory
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
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
private enum class SelectMode { Move, Resize, Marquee, None }
private enum class Corner { TL, TR, BL, BR }

private fun screenToCanvas(point: Offset, offset: Offset, scale: Float): Offset =
    Offset((point.x - offset.x) / scale, (point.y - offset.y) / scale)

private fun canvasToScreen(point: Offset, offset: Offset, scale: Float): Offset =
    Offset(point.x * scale + offset.x, point.y * scale + offset.y)

private fun normalizedRect(a: Offset, b: Offset): Rect =
    Rect(min(a.x, b.x), min(a.y, b.y), max(a.x, b.x), max(a.y, b.y))

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
    pendingInsertAsset: String? = null,
    onPendingInsertConsumed: () -> Unit = {},
    useDarkTheme: Boolean = false,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    // Geçici (canlı) çizim durumu — pointer döngüsü tarafından güncellenir, render tarafından okunur.
    var liveStrokePoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var liveStrokePressures by remember { mutableStateOf<List<Float>>(emptyList()) }
    var eraserCursor by remember { mutableStateOf<Offset?>(null) }
    var marqueeRect by remember { mutableStateOf<Rect?>(null) }

    var showColorMenu by remember { mutableStateOf(false) }
    var showWidthMenu by remember { mutableStateOf(false) }
    var showEraserMenu by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

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

    // Eski (legacy) arka plan sayfa görseli (canvas.png / canvas_page_N.png)
    var background by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(noteId, controller.safePageIndex) {
        background = withContext(Dispatchers.IO) {
            val dir = CanvasAssets.noteDir(filesDir, noteId)
            val file = if (controller.safePageIndex == 0) File(dir, "canvas.png") else File(dir, "canvas_page_${controller.safePageIndex}.png")
            if (file.exists()) BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() else null
        }
    }

    // Tahta çıktısı / galeri görseli: merkezi ve seçili olarak yerleştir (canvasSize bilindiğinde).
    LaunchedEffect(pendingInsertAsset, canvasSize) {
        val asset = pendingInsertAsset
        if (asset != null && canvasSize.width > 0 && canvasSize.height > 0) {
            val file = CanvasAssets.imageFile(filesDir, noteId, asset)
            val bmp = withContext(Dispatchers.IO) {
                if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
            }
            if (bmp != null) {
                val sc = controller.viewportScale
                val maxW = (canvasSize.width / sc) * 0.78f
                val maxH = (canvasSize.height / sc) * 0.72f
                val ratio = bmp.height.toFloat() / bmp.width.toFloat().coerceAtLeast(1f)
                var w = min(maxW, bmp.width.toFloat())
                var h = w * ratio
                if (h > maxH) { h = maxH; w = h / ratio.coerceAtLeast(0.0001f) }
                val center = screenToCanvas(
                    Offset(canvasSize.width / 2f, canvasSize.height / 2f),
                    controller.viewportOffset, sc
                )
                bitmapCache[asset] = bmp.asImageBitmap()
                controller.addElement(
                    ImageElement(asset = asset, left = center.x - w / 2f, top = center.y - h / 2f, width = w, height = h),
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
            .background(if (useDarkTheme) Color.Black else Color.White)
            .onSizeChanged { canvasSize = it }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .pointerInput(controller.tool, input, controller.safePageIndex) {
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
                        var resizeAnchor = Offset.Zero
                        var startBounds: Rect? = null
                        var sessionStarted = false
                        var erasing = false
                        var totalDrag = 0f
                        val startCanvas = screenToCanvas(firstDown.position, controller.viewportOffset, controller.viewportScale)

                        // İlk dokunuş aksiyonunu belirle
                        action = decideAction(firstDown.type)
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
                                        val off = controller.viewportOffset; val sc = controller.viewportScale
                                        val screenRect = Rect(
                                            canvasToScreen(Offset(bounds.left, bounds.top), off, sc),
                                            canvasToScreen(Offset(bounds.right, bounds.bottom), off, sc)
                                        )
                                        selectMode = if (screenRect.contains(firstDown.position)) SelectMode.Move else SelectMode.Marquee
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
                                    pinching = true
                                    liveStrokePoints = emptyList(); liveStrokePressures = emptyList()
                                    eraserCursor = null; marqueeRect = null
                                    if (erasing) { controller.endErase(); erasing = false }
                                    if (sessionStarted) { controller.endTransformSession(); sessionStarted = false }
                                    prevCentroid = null; prevDist = 0f
                                }
                                val p0 = pressed[0].position; val p1 = pressed[1].position
                                val centroid = Offset((p0.x + p1.x) / 2f, (p0.y + p1.y) / 2f)
                                val dist = (p0 - p1).getDistance().coerceAtLeast(1f)
                                val pc = prevCentroid
                                if (pc != null && prevDist > 0f) {
                                    val zoom = (dist / prevDist).coerceIn(0.8f, 1.25f)
                                    val oldScale = controller.viewportScale
                                    var newScale = (oldScale * zoom).coerceIn(input.minZoom, input.maxZoom)
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
                                    controller.viewportOffset = newOffset
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
                                        liveStrokePoints = liveStrokePoints + canvasPt
                                        liveStrokePressures = liveStrokePressures + pressureOf(ch.type, ch.pressure)
                                    }
                                    SingleAction.Erase -> {
                                        if (!erasing) { controller.beginErase(); erasing = true }
                                        eraserCursor = canvasPt
                                        controller.eraseStrokesAt(canvasPt, controller.eraserSize)
                                    }
                                    SingleAction.Pan -> {
                                        controller.viewportOffset = controller.viewportOffset + delta
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
                                        SelectMode.Marquee -> { marqueeRect = normalizedRect(startCanvas, canvasPt) }
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
                            SingleAction.SelectInteract -> when (selectMode) {
                                SelectMode.Move, SelectMode.Resize -> {
                                    if (sessionStarted) { controller.endTransformSession(); sessionStarted = false }
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
            withTransform({ translate(off.x, off.y); scale(sc, sc, pivot = Offset.Zero) }) {
                background?.let { bg ->
                    drawImage(
                        image = bg,
                        dstSize = IntSize(bg.width, bg.height),
                        colorFilter = if (useDarkTheme) invertFilter else null
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

            // Seçim kutusu + tutamaklar (ekran uzayında sabit boyutlu)
            val selBounds = controller.selectionBounds()
            if (selBounds != null && controller.tool == CanvasTool.Select) {
                val tl = canvasToScreen(Offset(selBounds.left, selBounds.top), off, sc)
                val br = canvasToScreen(Offset(selBounds.right, selBounds.bottom), off, sc)
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
            }
        }

        // Dock'lanan araç çubuğu
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
                            Slider(value = controller.strokeWidth, onValueChange = { controller.strokeWidth = it }, valueRange = 2f..36f)
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
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp),
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
    val draw: DrawScope.() -> Unit = {
        drawImage(
            image = bitmap,
            dstOffset = IntOffset(element.left.roundToInt(), element.top.roundToInt()),
            dstSize = IntSize(element.width.roundToInt().coerceAtLeast(1), element.height.roundToInt().coerceAtLeast(1))
        )
    }
    if (element.rotation != 0f) rotate(element.rotation, pivot = element.center) { draw() } else draw()
}

// --- Araç çubuğu düğmesi ---

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
