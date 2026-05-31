package com.board2notes.app.presentation.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import kotlin.math.roundToInt

private val PRESET_COLORS = listOf(
    Color(0xFF000000), Color(0xFF475569), Color(0xFF94A3B8),
    Color(0xFFEF4444), Color(0xFFF97316), Color(0xFFF59E0B),
    Color(0xFF10B981), Color(0xFF06B6D4), Color(0xFF2563EB),
    Color(0xFF7C3AED), Color(0xFFDB2777), Color(0xFFFFFFFF)
)

fun colorToHsv(color: Color): FloatArray {
    val hsv = FloatArray(3)
    android.graphics.Color.RGBToHSV(
        (color.red * 255f).roundToInt().coerceIn(0, 255),
        (color.green * 255f).roundToInt().coerceIn(0, 255),
        (color.blue * 255f).roundToInt().coerceIn(0, 255),
        hsv
    )
    return hsv
}

/**
 * Saf Compose HSV renk seçici: önayar paleti + son kullanılanlar + doygunluk/parlaklık karesi + ton kaydırıcısı.
 * Her değişiklikte [onColor] çağrılır (anlık önizleme için).
 */
@Composable
fun CanvasColorPicker(
    initial: Color,
    recents: List<Color>,
    onColor: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    val startHsv = remember(initial) { colorToHsv(initial) }
    var hue by remember { mutableFloatStateOf(startHsv[0]) }
    var sat by remember { mutableFloatStateOf(startHsv[1]) }
    var value by remember { mutableFloatStateOf(startHsv[2]) }

    fun emit() { onColor(Color.hsv(hue.coerceIn(0f, 360f), sat.coerceIn(0f, 1f), value.coerceIn(0f, 1f))) }

    Column(modifier = modifier.width(260.dp).padding(12.dp)) {
        Text("Renk", style = androidx.compose.material3.MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))

        // Doygunluk / Parlaklık karesi
        val hueColor = Color.hsv(hue, 1f, 1f)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(12.dp))
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        sat = (offset.x / size.width).coerceIn(0f, 1f)
                        value = (1f - offset.y / size.height).coerceIn(0f, 1f)
                        emit()
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        sat = (change.position.x / size.width).coerceIn(0f, 1f)
                        value = (1f - change.position.y / size.height).coerceIn(0f, 1f)
                        emit()
                    }
                }
        ) {
            Canvas(Modifier.matchParentSize()) {
                drawRect(brush = Brush.horizontalGradient(listOf(Color.White, hueColor)))
                drawRect(brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                val cx = sat * size.width
                val cy = (1f - value) * size.height
                drawCircle(Color.White, radius = 9f, center = Offset(cx, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f))
                drawCircle(Color.Black, radius = 12f, center = Offset(cx, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f))
            }
        }

        Spacer(Modifier.height(12.dp))

        // Ton kaydırıcısı
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
                .clip(CircleShape)
                .pointerInput(Unit) {
                    detectTapGestures { offset -> hue = (offset.x / size.width * 360f).coerceIn(0f, 360f); emit() }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ -> hue = (change.position.x / size.width * 360f).coerceIn(0f, 360f); emit() }
                }
        ) {
            Canvas(Modifier.matchParentSize()) {
                val hueColors = (0..360 step 30).map { Color.hsv(it.toFloat(), 1f, 1f) }
                drawRect(brush = Brush.horizontalGradient(hueColors))
                val x = hue / 360f * size.width
                drawCircle(Color.White, radius = size.height / 2f - 2f, center = Offset(x, size.height / 2f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f))
            }
        }

        Spacer(Modifier.height(14.dp))

        // Önayar paleti
        Text("Paletten", style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(6.dp))
        SwatchGrid(colors = PRESET_COLORS) { picked ->
            val hsv = colorToHsv(picked); hue = hsv[0]; sat = hsv[1]; value = hsv[2]; onColor(picked)
        }

        if (recents.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text("Son kullanılan", style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(6.dp))
            SwatchGrid(colors = recents) { picked ->
                val hsv = colorToHsv(picked); hue = hsv[0]; sat = hsv[1]; value = hsv[2]; onColor(picked)
            }
        }
    }
}

@Composable
private fun SwatchGrid(colors: List<Color>, onPick: (Color) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        colors.chunked(6).forEach { rowColors ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                rowColors.forEach { swatch ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(swatch)
                            .border(1.dp, Color(0x22000000), CircleShape)
                            .clickable { onPick(swatch) }
                    )
                }
            }
        }
    }
}
