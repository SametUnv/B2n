package com.board2notes.app.presentation.canvas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

enum class ToolbarDock { Top, Bottom, Left, Right }

/**
 * Kenara yapışan, sürüklenebilir ve yönü (yatay/dikey) dock'a göre değişen yüzen araç çubuğu.
 * Sürükleme tutamağından (grip) tutulup ekranın herhangi bir yerine taşınır; bırakıldığında en
 * yakın kenara snap olur. Üst/alt'ta yatay (Row), sol/sağ'da dikey (Column) dizilir.
 *
 * [content] verilen `vertical` bayrağına göre kendini uyarlayabilir (popup yönü vb.).
 */
@Composable
fun DockableToolbar(
    containerWidthPx: Float,
    containerHeightPx: Float,
    modifier: Modifier = Modifier,
    content: @Composable (vertical: Boolean) -> Unit
) {
    var dock by remember { mutableStateOf(ToolbarDock.Top) }
    var alongFraction by remember { mutableStateOf(0.5f) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    var dragging by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    val vertical = dock == ToolbarDock.Left || dock == ToolbarDock.Right
    val margin = 12f

    fun dockedOffset(): Offset {
        val w = size.width.toFloat()
        val h = size.height.toFloat()
        return when (dock) {
            ToolbarDock.Top -> Offset((alongFraction * containerWidthPx - w / 2f).coerceIn(margin, (containerWidthPx - w - margin).coerceAtLeast(margin)), margin)
            ToolbarDock.Bottom -> Offset((alongFraction * containerWidthPx - w / 2f).coerceIn(margin, (containerWidthPx - w - margin).coerceAtLeast(margin)), (containerHeightPx - h - margin).coerceAtLeast(margin))
            ToolbarDock.Left -> Offset(margin, (alongFraction * containerHeightPx - h / 2f).coerceIn(margin, (containerHeightPx - h - margin).coerceAtLeast(margin)))
            ToolbarDock.Right -> Offset((containerWidthPx - w - margin).coerceAtLeast(margin), (alongFraction * containerHeightPx - h / 2f).coerceIn(margin, (containerHeightPx - h - margin).coerceAtLeast(margin)))
        }
    }

    val renderOffset = if (dragging) dragOffset else dockedOffset()

    Surface(
        modifier = modifier
            .offset { IntOffset(renderOffset.x.roundToInt(), renderOffset.y.roundToInt()) }
            .onSizeChanged { size = it }
            // Araç çubuğunun boş alanlarına yapılan dokunuşların arkadaki canvas'a düşüp
            // çizim yapmasını engelle. Yalnızca ilk "down" olayını tüket; hareket olayları
            // serbest kalsın ki grip sürüklemesi ve düğme tıklamaları bozulmasın.
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.forEach { if (it.changedToDown()) it.consume() }
                    }
                }
            },
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 10.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
    ) {
        val gripModifier = Modifier.pointerInput(containerWidthPx, containerHeightPx) {
            detectDragGestures(
                onDragStart = {
                    dragging = true
                    dragOffset = dockedOffset()
                },
                onDrag = { change, amount ->
                    change.consume()
                    dragOffset = Offset(
                        (dragOffset.x + amount.x).coerceIn(0f, (containerWidthPx - size.width).coerceAtLeast(0f)),
                        (dragOffset.y + amount.y).coerceIn(0f, (containerHeightPx - size.height).coerceAtLeast(0f))
                    )
                },
                onDragEnd = {
                    // Snap'i çubuğun en yakın KENARINA göre seç (merkez yerine kenar mesafesi),
                    // böylece geniş yatay çubuk sola/sağa sürüklenince dikeye dönebilir.
                    val w = size.width.toFloat()
                    val h = size.height.toFloat()
                    val centerX = dragOffset.x + w / 2f
                    val centerY = dragOffset.y + h / 2f
                    val dLeft = dragOffset.x
                    val dRight = containerWidthPx - (dragOffset.x + w)
                    val dTop = dragOffset.y
                    val dBottom = containerHeightPx - (dragOffset.y + h)
                    val nearest = minOf(dLeft, dRight, dTop, dBottom)
                    dock = when (nearest) {
                        dLeft -> ToolbarDock.Left
                        dRight -> ToolbarDock.Right
                        dTop -> ToolbarDock.Top
                        else -> ToolbarDock.Bottom
                    }
                    alongFraction = if (dock == ToolbarDock.Left || dock == ToolbarDock.Right) {
                        (centerY / containerHeightPx).coerceIn(0f, 1f)
                    } else {
                        (centerX / containerWidthPx).coerceIn(0f, 1f)
                    }
                    dragging = false
                }
            )
        }

        if (vertical) {
            Column(
                modifier = Modifier.padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Box(gripModifier.size(34.dp, 22.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.DragIndicator, contentDescription = "Taşı", tint = Color(0xFF94A3B8))
                }
                content(true)
            }
        } else {
            Row(
                modifier = Modifier.padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Box(gripModifier.size(22.dp, 34.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.DragIndicator, contentDescription = "Taşı", tint = Color(0xFF94A3B8))
                }
                content(false)
            }
        }
    }
}
