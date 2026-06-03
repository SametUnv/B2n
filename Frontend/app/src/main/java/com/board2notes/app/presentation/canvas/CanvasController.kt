package com.board2notes.app.presentation.canvas

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import java.util.UUID
import kotlin.math.max

enum class CanvasTool { Pen, Eraser, Select, Pan }
enum class CropEdge { Left, Top, Right, Bottom }

/**
 * Canvas editörünün tüm durumunu tutan, recomposition'lar arası yaşayan denetleyici.
 * `remember(noteKey) { CanvasController(...) }` ile oluşturulur. Kalıcılaştırılması gereken
 * her değişiklikten sonra [onChange] çağrılır (editör body'e serileştirip ViewModel'e yazar).
 */
class CanvasController(
    initialPages: List<CanvasPage>,
    private val onChange: (List<CanvasPage>) -> Unit
) {
    var pages by mutableStateOf(initialPages.ifEmpty { listOf(emptyList()) })
        private set
    var pageIndex by mutableIntStateOf(0)
        private set

    var tool by mutableStateOf(CanvasTool.Pen)
    var color by mutableStateOf(Color.Black)
    var strokeWidth by mutableStateOf(30f)
    var eraserSize by mutableStateOf(48f)

    var selection by mutableStateOf<Set<String>>(emptySet())

    var viewportScale by mutableStateOf(1f)
    var viewportOffset by mutableStateOf(Offset.Zero)

    private val undoStack = ArrayDeque<List<CanvasPage>>()
    private val redoStack = ArrayDeque<List<CanvasPage>>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()
    val pageCount: Int get() = pages.size
    val safePageIndex: Int get() = pageIndex.coerceIn(0, (pages.size - 1).coerceAtLeast(0))

    fun currentPage(): CanvasPage = pages.getOrElse(safePageIndex) { emptyList() }

    fun elementById(id: String): CanvasElement? = currentPage().firstOrNull { it.id == id }

    fun selectedElements(): List<CanvasElement> = currentPage().filter { it.id in selection }

    // --- Sayfa işlemleri ---

    fun goToPage(index: Int) {
        pageIndex = index.coerceIn(0, pages.lastIndex)
        selection = emptySet()
    }

    fun addPage() {
        pushUndo()
        pages = pages + listOf(emptyList())
        pageIndex = pages.lastIndex
        selection = emptySet()
        viewportScale = 1f
        viewportOffset = Offset.Zero
        onChange(pages)
    }

    fun clearCurrentPage() {
        replaceCurrentPage(emptyList())
        selection = emptySet()
    }

    // --- Eleman işlemleri ---

    fun addElement(element: CanvasElement, select: Boolean = false) {
        replaceCurrentPage(currentPage() + element)
        if (select) selection = setOf(element.id)
    }

    fun removeElements(ids: Set<String>) {
        if (ids.isEmpty()) return
        replaceCurrentPage(currentPage().filterNot { it.id in ids })
        selection = selection - ids
    }

    fun replaceCurrentPage(newElements: CanvasPage) {
        pushUndo()
        pages = pages.toMutableList().also { list ->
            while (list.size <= safePageIndex) list.add(emptyList())
            list[safePageIndex] = newElements
        }
        onChange(pages)
    }

    /** Sürükleme bittiğinde tek seferde çağrılır; ara taşıma adımları undo yığınını şişirmesin diye. */
    private fun replaceCurrentPageNoUndo(newElements: CanvasPage) {
        pages = pages.toMutableList().also { list ->
            while (list.size <= safePageIndex) list.add(emptyList())
            list[safePageIndex] = newElements
        }
    }

    // --- Seçim ---

    fun clearSelection() { selection = emptySet() }

    fun selectAt(point: Offset): Boolean {
        // En üstteki (son çizilen) elemanı seç
        val hit = currentPage().lastOrNull { hitTest(it, point) }
        selection = if (hit != null) setOf(hit.id) else emptySet()
        return hit != null
    }

    fun selectInRect(rect: Rect) {
        selection = currentPage().filter { rectsOverlap(elementBounds(it), rect) }.map { it.id }.toSet()
    }

    fun selectionBounds(): Rect? = unionBounds(selectedElements())

    // --- Taşıma / Ölçekleme oturumu (canlı sürükleme) ---
    // Sürükleme başında orijinal sayfa snapshot'lanır; her karede dönüşüm orijinalden hesaplanır,
    // böylece tekrarlanan çağrılar birikmez. Oturum başlarken bir undo noktası kaydedilir.

    private var sessionOriginalPage: CanvasPage? = null
    private var cropOriginalPage: CanvasPage? = null

    fun beginTransformSession() {
        if (selection.isEmpty()) return
        pushUndo()
        sessionOriginalPage = currentPage()
    }

    fun sessionTranslate(delta: Offset) {
        val original = sessionOriginalPage ?: return
        replaceCurrentPageNoUndo(original.map { element ->
            if (element.id in selection) translateElement(element, delta) else element
        })
    }

    fun sessionScale(anchor: Offset, scaleX: Float, scaleY: Float) {
        val original = sessionOriginalPage ?: return
        replaceCurrentPageNoUndo(original.map { element ->
            if (element.id in selection) scaleElement(element, anchor, scaleX, scaleY) else element
        })
    }

    fun endTransformSession() {
        if (sessionOriginalPage == null) return
        sessionOriginalPage = null
        onChange(pages)
    }

    val inTransformSession: Boolean get() = sessionOriginalPage != null

    fun beginCropSession() {
        val hasSelectedImage = currentPage().any { it is ImageElement && it.id in selection }
        if (!hasSelectedImage) return
        pushUndo()
        cropOriginalPage = currentPage()
    }

    fun sessionCropSelectedImages(edge: CropEdge, delta: Offset) {
        val original = cropOriginalPage ?: return
        replaceCurrentPageNoUndo(original.map { element ->
            if (element is ImageElement && element.id in selection) {
                cropImageElement(element, edge, delta)
            } else {
                element
            }
        })
    }

    fun endCropSession() {
        if (cropOriginalPage == null) return
        cropOriginalPage = null
        onChange(pages)
    }

    // --- Silgi oturumu (tek undo adımı, canlı silme) ---

    private var hasEraseChanges = false

    fun beginErase() {
        pushUndo()
        hasEraseChanges = false
    }

    fun eraseStrokesAt(point: Offset, radius: Float) {
        val original = currentPage()
        var changed = false
        val newPage = mutableListOf<CanvasElement>()
        
        original.forEach { element ->
            if (element is StrokeElement) {
                val subStrokes = eraseStroke(element, point, radius)
                if (subStrokes.size != 1 || subStrokes[0].points.size != element.points.size) {
                    newPage.addAll(subStrokes)
                    changed = true
                } else {
                    newPage.add(element)
                }
            } else {
                newPage.add(element)
            }
        }
        
        if (changed) {
            replaceCurrentPageNoUndo(newPage)
            hasEraseChanges = true
        }
    }

    fun endErase() {
        if (hasEraseChanges) {
            onChange(pages)
        }
    }

    private fun eraseStroke(stroke: StrokeElement, center: Offset, radius: Float): List<StrokeElement> {
        val r2 = radius * radius
        val subStrokes = mutableListOf<StrokeElement>()
        val currentPoints = mutableListOf<Offset>()
        val currentPressures = mutableListOf<Float>()
        
        for (i in stroke.points.indices) {
            val pt = stroke.points[i]
            val pressure = stroke.pressures.getOrNull(i) ?: 1f
            val isTouched = (pt - center).getDistanceSquared() <= r2
            
            if (isTouched) {
                if (currentPoints.isNotEmpty()) {
                    subStrokes.add(
                        stroke.copy(
                            id = UUID.randomUUID().toString(),
                            points = currentPoints.toList(),
                            pressures = if (stroke.pressures.isNotEmpty()) currentPressures.toList() else emptyList()
                        )
                    )
                    currentPoints.clear()
                    currentPressures.clear()
                }
            } else {
                currentPoints.add(pt)
                if (stroke.pressures.isNotEmpty()) {
                    currentPressures.add(pressure)
                }
            }
        }
        
        if (currentPoints.isNotEmpty()) {
            subStrokes.add(
                stroke.copy(
                    id = UUID.randomUUID().toString(),
                    points = currentPoints.toList(),
                    pressures = if (stroke.pressures.isNotEmpty()) currentPressures.toList() else emptyList()
                )
            )
        }
        
        return subStrokes
    }

    fun deleteSelection() {
        if (selection.isEmpty()) return
        removeElements(selection)
    }

    fun restoreSelectedImagesToOriginalPlacement() {
        if (selection.isEmpty()) return
        val selectedImageIds = currentPage()
            .filterIsInstance<ImageElement>()
            .filter { it.id in selection }
            .map { it.id }
            .toSet()
        if (selectedImageIds.isEmpty()) return
        replaceCurrentPage(currentPage().map { element ->
            if (element is ImageElement && element.id in selectedImageIds) {
                element.copy(
                    left = element.initialLeft,
                    top = element.initialTop,
                    width = element.initialWidth,
                    height = element.initialHeight,
                    rotation = 0f,
                    cropLeft = 0f,
                    cropTop = 0f,
                    cropRight = 0f,
                    cropBottom = 0f
                )
            } else {
                element
            }
        })
        selection = selectedImageIds
    }

    fun rotateSelectedImages(degrees: Float = 90f) {
        if (selection.isEmpty()) return
        val selectedImageIds = currentPage()
            .filterIsInstance<ImageElement>()
            .filter { it.id in selection }
            .map { it.id }
            .toSet()
        if (selectedImageIds.isEmpty()) return
        replaceCurrentPage(currentPage().map { element ->
            if (element is ImageElement && element.id in selectedImageIds) {
                val rotation = (element.rotation + degrees) % 360f
                element.copy(rotation = if (rotation < 0f) rotation + 360f else rotation)
            } else {
                element
            }
        })
        selection = selectedImageIds
    }

    /**
     * Bir görsele OCR ("metne çevir") sonucunu iliştirir. Görünür bir dönüşüm olmadığı için
     * undo yığınını kirletmez; ancak [onChange] ile kalıcılaştırılır (canvas JSON'una yazılır).
     */
    fun setImageOcrText(id: String, ocrText: String) {
        val page = currentPage()
        if (page.none { it is ImageElement && it.id == id }) return
        replaceCurrentPageNoUndo(page.map { element ->
            if (element is ImageElement && element.id == id) element.copy(ocrText = ocrText) else element
        })
        onChange(pages)
    }

    /** AI açıklama için: nottaki TÜM sayfalarda OCR'lanmış görsel metinlerini birleştirir. */
    fun imagesOcrCombined(): String =
        pages.flatten()
            .filterIsInstance<ImageElement>()
            .mapNotNull { it.ocrText.takeIf { text -> text.isNotBlank() } }
            .joinToString("\n\n---\n\n")

    // --- Undo / Redo ---

    fun undo() {
        if (undoStack.isEmpty()) return
        redoStack.addLast(pages)
        pages = undoStack.removeLast()
        pageIndex = pageIndex.coerceIn(0, pages.lastIndex)
        selection = emptySet()
        onChange(pages)
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        undoStack.addLast(pages)
        pages = redoStack.removeLast()
        pageIndex = pageIndex.coerceIn(0, pages.lastIndex)
        selection = emptySet()
        onChange(pages)
    }

    private fun pushUndo() {
        undoStack.addLast(pages)
        if (undoStack.size > MAX_HISTORY) undoStack.removeFirst()
        redoStack.clear()
    }

    companion object {
        private const val MAX_HISTORY = 60
        private const val MIN_IMAGE_SIZE = 32f
        private const val MAX_CROP_TOTAL = 0.96f

        fun translateElement(element: CanvasElement, delta: Offset): CanvasElement = when (element) {
            is StrokeElement -> element.copy(points = element.points.map { Offset(it.x + delta.x, it.y + delta.y) })
            is ImageElement -> element.copy(left = element.left + delta.x, top = element.top + delta.y)
        }

        fun scaleElement(element: CanvasElement, anchor: Offset, sx: Float, sy: Float): CanvasElement {
            fun sxp(x: Float) = anchor.x + (x - anchor.x) * sx
            fun syp(y: Float) = anchor.y + (y - anchor.y) * sy
            return when (element) {
                is StrokeElement -> element.copy(
                    points = element.points.map { Offset(sxp(it.x), syp(it.y)) },
                    baseWidth = (element.baseWidth * (sx + sy) / 2f).coerceAtLeast(1f)
                )
                is ImageElement -> {
                    val newLeft = sxp(element.left)
                    val newTop = syp(element.top)
                    element.copy(
                        left = newLeft,
                        top = newTop,
                        width = max(8f, element.width * sx),
                        height = max(8f, element.height * sy)
                    )
                }
            }
        }

        private fun cropImageElement(element: ImageElement, edge: CropEdge, delta: Offset): ImageElement {
            val cropLeft = element.cropLeft.coerceIn(0f, MAX_CROP_TOTAL)
            val cropRight = element.cropRight.coerceIn(0f, MAX_CROP_TOTAL - cropLeft)
            val cropTop = element.cropTop.coerceIn(0f, MAX_CROP_TOTAL)
            val cropBottom = element.cropBottom.coerceIn(0f, MAX_CROP_TOTAL - cropTop)
            val visibleX = (1f - cropLeft - cropRight).coerceAtLeast(0.04f)
            val visibleY = (1f - cropTop - cropBottom).coerceAtLeast(0.04f)
            val sourceDisplayWidth = element.width / visibleX
            val sourceDisplayHeight = element.height / visibleY

            return when (edge) {
                CropEdge.Left -> {
                    val dx = delta.x.coerceIn(-cropLeft * sourceDisplayWidth, element.width - MIN_IMAGE_SIZE)
                    element.copy(
                        left = element.left + dx,
                        width = (element.width - dx).coerceAtLeast(MIN_IMAGE_SIZE),
                        cropLeft = (cropLeft + dx / sourceDisplayWidth).coerceIn(0f, MAX_CROP_TOTAL - cropRight),
                        cropRight = cropRight,
                        cropTop = cropTop,
                        cropBottom = cropBottom
                    )
                }
                CropEdge.Right -> {
                    val dx = delta.x.coerceIn(-(element.width - MIN_IMAGE_SIZE), cropRight * sourceDisplayWidth)
                    element.copy(
                        width = (element.width + dx).coerceAtLeast(MIN_IMAGE_SIZE),
                        cropLeft = cropLeft,
                        cropRight = (cropRight - dx / sourceDisplayWidth).coerceIn(0f, MAX_CROP_TOTAL - cropLeft),
                        cropTop = cropTop,
                        cropBottom = cropBottom
                    )
                }
                CropEdge.Top -> {
                    val dy = delta.y.coerceIn(-cropTop * sourceDisplayHeight, element.height - MIN_IMAGE_SIZE)
                    element.copy(
                        top = element.top + dy,
                        height = (element.height - dy).coerceAtLeast(MIN_IMAGE_SIZE),
                        cropLeft = cropLeft,
                        cropRight = cropRight,
                        cropTop = (cropTop + dy / sourceDisplayHeight).coerceIn(0f, MAX_CROP_TOTAL - cropBottom),
                        cropBottom = cropBottom
                    )
                }
                CropEdge.Bottom -> {
                    val dy = delta.y.coerceIn(-(element.height - MIN_IMAGE_SIZE), cropBottom * sourceDisplayHeight)
                    element.copy(
                        height = (element.height + dy).coerceAtLeast(MIN_IMAGE_SIZE),
                        cropLeft = cropLeft,
                        cropRight = cropRight,
                        cropTop = cropTop,
                        cropBottom = (cropBottom - dy / sourceDisplayHeight).coerceIn(0f, MAX_CROP_TOTAL - cropTop)
                    )
                }
            }
        }

        fun hitTest(element: CanvasElement, point: Offset): Boolean = when (element) {
            is ImageElement -> element.rect.contains(point)
            is StrokeElement -> {
                val tolerance = max(element.baseWidth, 24f)
                element.points.any { (it - point).getDistanceSquared() <= tolerance * tolerance } ||
                    elementBounds(element).inflate(tolerance / 2f).contains(point)
            }
        }

        private fun rectsOverlap(a: Rect, b: Rect): Boolean =
            a.left <= b.right && a.right >= b.left && a.top <= b.bottom && a.bottom >= b.top

        fun strokeTouched(stroke: StrokeElement, point: Offset, radius: Float): Boolean {
            val r2 = radius * radius
            return stroke.points.any { (it - point).getDistanceSquared() <= r2 }
        }
    }
}
