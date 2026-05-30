package com.board2notes.app.presentation.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.board2notes.app.domain.model.Quad

@Composable
fun PreviewImage(
    bitmap: Bitmap,
    modifier: Modifier = Modifier,
    contentDescription: String = "Görüntü"
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 1.dp
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
fun ImageWithQuad(
    bitmap: Bitmap,
    quad: Quad?,
    modifier: Modifier = Modifier,
    onCornerDrag: ((index: Int, x: Float, y: Float) -> Unit)? = null
) {
    val quadColor = MaterialTheme.colorScheme.primary
    val latestQuad by rememberUpdatedState(quad)
    val latestCornerDrag by rememberUpdatedState(onCornerDrag)
    val dragModifier = if (onCornerDrag != null) {
        Modifier.pointerInput(bitmap) {
            var activeCorner: Int? = null
            fun mappedPoints(currentQuad: Quad): List<Offset> {
                val scale = minOf(size.width / bitmap.width.toFloat(), size.height / bitmap.height.toFloat())
                val dx = (size.width - bitmap.width * scale) / 2f
                val dy = (size.height - bitmap.height * scale) / 2f
                fun map(x: Float, y: Float) = Offset(dx + x * scale, dy + y * scale)
                return listOf(
                    map(currentQuad.topLeft.x, currentQuad.topLeft.y),
                    map(currentQuad.topRight.x, currentQuad.topRight.y),
                    map(currentQuad.bottomRight.x, currentQuad.bottomRight.y),
                    map(currentQuad.bottomLeft.x, currentQuad.bottomLeft.y)
                )
            }
            fun toImagePoint(position: Offset): Offset {
                val scale = minOf(size.width / bitmap.width.toFloat(), size.height / bitmap.height.toFloat())
                val dx = (size.width - bitmap.width * scale) / 2f
                val dy = (size.height - bitmap.height * scale) / 2f
                return Offset(
                    x = ((position.x - dx) / scale).coerceIn(0f, bitmap.width.toFloat()),
                    y = ((position.y - dy) / scale).coerceIn(0f, bitmap.height.toFloat())
                )
            }
            detectDragGestures(
                onDragStart = { position ->
                    val currentQuad = latestQuad ?: return@detectDragGestures
                    val touchRadius = 42.dp.toPx()
                    val touchRadiusSquared = touchRadius * touchRadius
                    activeCorner = mappedPoints(currentQuad)
                        .mapIndexed { index, point ->
                            val dx = point.x - position.x
                            val dy = point.y - position.y
                            index to (dx * dx + dy * dy)
                        }
                        .filter { it.second <= touchRadiusSquared }
                        .minByOrNull { it.second }
                        ?.first
                },
                onDragEnd = { activeCorner = null },
                onDragCancel = { activeCorner = null },
                onDrag = { change, _ ->
                    val index = activeCorner ?: return@detectDragGestures
                    val imagePoint = toImagePoint(change.position)
                    latestCornerDrag?.invoke(index, imagePoint.x, imagePoint.y)
                    change.consume()
                }
            )
        }
    } else {
        Modifier
    }
    Box(modifier = modifier.fillMaxWidth()) {
        PreviewImage(bitmap = bitmap)
        if (quad != null) {
            Canvas(modifier = Modifier.matchParentSize().then(dragModifier)) {
                val scale = minOf(size.width / bitmap.width, size.height / bitmap.height)
                val dx = (size.width - bitmap.width * scale) / 2f
                val dy = (size.height - bitmap.height * scale) / 2f
                fun map(x: Float, y: Float) = Offset(dx + x * scale, dy + y * scale)
                val mappedPoints = listOf(
                    map(quad.topLeft.x, quad.topLeft.y),
                    map(quad.topRight.x, quad.topRight.y),
                    map(quad.bottomRight.x, quad.bottomRight.y),
                    map(quad.bottomLeft.x, quad.bottomLeft.y)
                )
                val path = Path().apply {
                    moveTo(mappedPoints[0].x, mappedPoints[0].y)
                    lineTo(mappedPoints[1].x, mappedPoints[1].y)
                    lineTo(mappedPoints[2].x, mappedPoints[2].y)
                    lineTo(mappedPoints[3].x, mappedPoints[3].y)
                    close()
                }
                drawPath(path = path, color = quadColor.copy(alpha = 0.10f))
                drawPath(
                    path = path,
                    color = quadColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                mappedPoints.forEach { point ->
                    drawCircle(quadColor.copy(alpha = 0.22f), radius = 13.dp.toPx(), center = point)
                    drawCircle(quadColor, radius = 8.dp.toPx(), center = point)
                    drawCircle(Color.White, radius = 3.5.dp.toPx(), center = point)
                }
            }
        }
    }
}

@Composable
fun SectionTitle(title: String, subtitle: String? = null) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        if (subtitle != null) {
            Spacer(Modifier.height(4.dp))
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun StatusBanner(message: String, busy: Boolean = false) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (busy) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.HourglassTop, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(text = message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun WarningText(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .padding(12.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium
    )
}
