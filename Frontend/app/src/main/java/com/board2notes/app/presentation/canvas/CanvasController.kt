package com.board2notes.app.presentation.canvas

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import kotlin.math.max

enum class CanvasTool { Pen, Eraser, Select }

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
    var strokeWidth by mutableStateOf(6f)
    var eraserSize by mutableStateOf(32f)

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

    // --- Silgi oturumu (tek undo adımı, canlı silme) ---

    private var eraseOriginalPage: CanvasPage? = null
    private val erasedIds = mutableSetOf<String>()

    fun beginErase() {
        pushUndo()
        eraseOriginalPage = currentPage()
        erasedIds.clear()
    }

    fun eraseStrokesAt(point: Offset, radius: Float) {
        val original = eraseOriginalPage ?: return
        var changed = false
        original.forEach { element ->
            if (element is StrokeElement && element.id !in erasedIds && strokeTouched(element, point, radius)) {
                erasedIds.add(element.id); changed = true
            }
        }
        if (changed) replaceCurrentPageNoUndo(original.filterNot { it.id in erasedIds })
    }

    fun endErase() {
        if (eraseOriginalPage == null) return
        eraseOriginalPage = null
        if (erasedIds.isNotEmpty()) onChange(pages)
    }

    fun deleteSelection() {
        if (selection.isEmpty()) return
        removeElements(selection)
    }

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
