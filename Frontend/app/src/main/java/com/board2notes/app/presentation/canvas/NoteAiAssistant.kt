package com.board2notes.app.presentation.canvas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** AI "anlat" özelliğinin durumu (ekran tarafından hoist edilir). */
sealed interface NoteAiState {
    data object Idle : NoteAiState
    data object Loading : NoteAiState
    data class Success(val text: String) : NoteAiState
    data class Error(val message: String) : NoteAiState
}

private enum class AiCorner { TopLeft, TopRight, BottomLeft, BottomRight }

private fun AiCorner.isLeft() = this == AiCorner.TopLeft || this == AiCorner.BottomLeft
private fun AiCorner.isTop() = this == AiCorner.TopLeft || this == AiCorner.TopRight

/**
 * Not sayfası üzerinde yüzen, dört köşeye sürüklenip yapışan AI butonu (FAB) ve buton
 * konumuna göre konumlanan açıklama pop-up'ı. Kök kutu dokunuşları tüketmez; yalnızca FAB
 * ve açık pop-up dokunuşu tüketir, böylece boş alanlar altındaki canvas ile etkileşime açık kalır.
 *
 * Tüm ağ/iş mantığı çağıran ekranda yaşar; bu bileşen yalnızca [state]'i gösterir ve
 * butona dokununca [onTogglePopup]'u tetikler.
 */
@Composable
fun NoteAiAssistant(
    state: NoteAiState,
    popupOpen: Boolean,
    onTogglePopup: () -> Unit,
    onDismiss: () -> Unit,
    onRegenerate: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var corner by remember { mutableStateOf(AiCorner.TopRight) }
    var dragging by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var popupSize by remember { mutableStateOf(IntSize.Zero) }

    val density = LocalDensity.current
    val fabSizeDp = 52.dp
    val fabSizePx = with(density) { fabSizeDp.toPx() }
    val marginPx = with(density) { 16.dp.toPx() }
    // Üst köşelerde CanvasEditor'ın sağ-üst tam ekran düğmesiyle (top:12, 42dp) çakışmayı önlemek için.
    val topInsetPx = with(density) { 64.dp.toPx() }
    val gapPx = with(density) { 10.dp.toPx() }

    fun cornerOffset(c: AiCorner): Offset {
        val w = containerSize.width.toFloat()
        val h = containerSize.height.toFloat()
        val rightX = (w - fabSizePx - marginPx).coerceAtLeast(marginPx)
        val bottomY = (h - fabSizePx - marginPx).coerceAtLeast(topInsetPx)
        return when (c) {
            AiCorner.TopLeft -> Offset(marginPx, topInsetPx)
            AiCorner.TopRight -> Offset(rightX, topInsetPx)
            AiCorner.BottomLeft -> Offset(marginPx, bottomY)
            AiCorner.BottomRight -> Offset(rightX, bottomY)
        }
    }

    Box(modifier = modifier.onSizeChanged { containerSize = it }) {
        if (containerSize.width <= 0 || containerSize.height <= 0) return@Box

        val fabPos = if (dragging) dragOffset else cornerOffset(corner)

        // --- Pop-up (FAB konumuna göre yerleşir) ---
        if (popupOpen) {
            val popupWidthDp = 320.dp
            val popupWidthPx = with(density) { popupWidthDp.toPx() }
            val maxPopupHeightPx = containerSize.height * 0.62f

            Surface(
                modifier = Modifier
                    .width(popupWidthDp)
                    .heightIn(max = with(density) { maxPopupHeightPx.toDp() })
                    .onSizeChanged { popupSize = it }
                    .offset {
                        val fabRight = fabPos.x + fabSizePx
                        val rawX = if (corner.isLeft()) fabPos.x else fabRight - popupWidthPx
                        val ph = popupSize.height.toFloat()
                        val rawY = if (corner.isTop()) fabPos.y + fabSizePx + gapPx else fabPos.y - ph - gapPx
                        val x = rawX.coerceIn(marginPx, (containerSize.width - popupWidthPx - marginPx).coerceAtLeast(marginPx))
                        val y = rawY.coerceIn(marginPx, (containerSize.height - ph - marginPx).coerceAtLeast(marginPx))
                        IntOffset(x.roundToInt(), y.roundToInt())
                    }
                    // Pop-up'a yapılan dokunuşların alttaki canvas'a düşmesini engelle.
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                event.changes.forEach { if (it.changedToDown()) it.consume() }
                            }
                        }
                    },
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                shadowElevation = 16.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
            ) {
                NoteAiPopupContent(state = state, onClose = onDismiss, onRegenerate = onRegenerate)
            }
        }

        // --- Sürüklenebilir AI butonu (FAB) ---
        val loading = state is NoteAiState.Loading
        Surface(
            modifier = Modifier
                .offset { IntOffset(fabPos.x.roundToInt(), fabPos.y.roundToInt()) }
                .size(fabSizeDp)
                .pointerInput(containerSize) {
                    detectDragGestures(
                        onDragStart = {
                            dragging = true
                            dragOffset = cornerOffset(corner)
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            dragOffset = Offset(
                                (dragOffset.x + amount.x).coerceIn(0f, (containerSize.width - fabSizePx).coerceAtLeast(0f)),
                                (dragOffset.y + amount.y).coerceIn(0f, (containerSize.height - fabSizePx).coerceAtLeast(0f))
                            )
                        },
                        onDragEnd = {
                            val cx = dragOffset.x + fabSizePx / 2f
                            val cy = dragOffset.y + fabSizePx / 2f
                            val left = cx < containerSize.width / 2f
                            val top = cy < containerSize.height / 2f
                            corner = when {
                                left && top -> AiCorner.TopLeft
                                !left && top -> AiCorner.TopRight
                                left && !top -> AiCorner.BottomLeft
                                else -> AiCorner.BottomRight
                            }
                            dragging = false
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onTogglePopup() })
                },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            shadowElevation = 8.dp
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = "AI ile anlat",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun NoteAiPopupContent(state: NoteAiState, onClose: () -> Unit, onRegenerate: (() -> Unit)?) {
    Column(modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "AI Açıklama",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (onRegenerate != null && (state is NoteAiState.Success || state is NoteAiState.Error)) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onRegenerate),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Yeniden Gönder",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(4.dp))
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Kapat",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(Modifier.size(8.dp))
        when (state) {
            is NoteAiState.Loading -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Açıklama hazırlanıyor…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            is NoteAiState.Error -> {
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.verticalScroll(rememberScrollState())
                )
            }
            is NoteAiState.Success -> {
                SelectionContainer(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = state.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            NoteAiState.Idle -> {
                Text(
                    text = "Bu nottaki görsellerin metinlerinden açıklama üretmek için AI butonuna dokunun.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
