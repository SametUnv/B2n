package com.board2notes.app.presentation.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
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
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth()) {
        PreviewImage(bitmap = bitmap)
        if (quad != null) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val scale = minOf(size.width / bitmap.width, size.height / bitmap.height)
                val dx = (size.width - bitmap.width * scale) / 2f
                val dy = (size.height - bitmap.height * scale) / 2f
                fun map(x: Float, y: Float) = Offset(dx + x * scale, dy + y * scale)
                val path = Path().apply {
                    moveTo(map(quad.topLeft.x, quad.topLeft.y).x, map(quad.topLeft.x, quad.topLeft.y).y)
                    lineTo(map(quad.topRight.x, quad.topRight.y).x, map(quad.topRight.x, quad.topRight.y).y)
                    lineTo(map(quad.bottomRight.x, quad.bottomRight.y).x, map(quad.bottomRight.x, quad.bottomRight.y).y)
                    lineTo(map(quad.bottomLeft.x, quad.bottomLeft.y).x, map(quad.bottomLeft.x, quad.bottomLeft.y).y)
                    close()
                }
                drawPath(
                    path = path,
                    color = Color(0xFF00A7B5),
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
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
