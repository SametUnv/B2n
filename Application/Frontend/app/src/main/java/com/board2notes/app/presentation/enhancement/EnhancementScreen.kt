package com.board2notes.app.presentation.enhancement

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.board2notes.app.core.ui.components.AppScaffold
import com.board2notes.app.core.ui.components.PrimaryButton
import com.board2notes.app.core.ui.components.SecondaryButton
import com.board2notes.app.core.ui.theme.PrimarySoft
import com.board2notes.app.domain.model.BoardMode
import com.board2notes.app.presentation.shared.BoardViewModel

@Composable
fun EnhancementScreen(
    viewModel: BoardViewModel,
    onBack: () -> Unit,
    onModeSelected: (BoardMode) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // ColorMatrix that mocks a "scanned / cleaned board" look:
    // desaturate then push contrast/brightness up.
    val enhancedFilter = remember {
        ColorFilter.colorMatrix(
            ColorMatrix().apply {
                setToSaturation(0f)
                // boost contrast & brightness
                val c = 1.6f
                val t = -60f
                timesAssign(
                    ColorMatrix(
                        floatArrayOf(
                            c, 0f, 0f, 0f, t,
                            0f, c, 0f, 0f, t,
                            0f, 0f, c, 0f, t,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                )
            }
        )
    }

    AppScaffold(title = "İyileştirme", onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Öncesi / Sonrası",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Tahtan temizlenip okunur hale getirildi.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LabeledImage(
                    label = "Orijinal",
                    uri = state.originalImageUrl,
                    modifier = Modifier.weight(1f)
                )
                LabeledImage(
                    label = "İyileştirilmiş",
                    uri = state.enhancedImageUrl ?: state.originalImageUrl,
                    colorFilter = enhancedFilter,
                    highlighted = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(28.dp))

            Text(
                text = "Nasıl bir not istersin?",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(14.dp))

            PrimaryButton(
                text = "OCR ile Not Oluştur",
                onClick = {
                    viewModel.selectMode(BoardMode.OCR_NOTE) { onModeSelected(BoardMode.OCR_NOTE) }
                },
                leadingIcon = Icons.Default.TextFields
            )
            Spacer(Modifier.height(12.dp))
            SecondaryButton(
                text = "Görsel Not Oluştur",
                onClick = {
                    viewModel.selectMode(BoardMode.VISUAL_NOTE) { onModeSelected(BoardMode.VISUAL_NOTE) }
                },
                leadingIcon = Icons.Default.Image
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun LabeledImage(
    label: String,
    uri: String?,
    modifier: Modifier = Modifier,
    colorFilter: ColorFilter? = null,
    highlighted: Boolean = false
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(MaterialTheme.shapes.medium)
                .background(PrimarySoft),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = uri,
                contentDescription = label,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                colorFilter = colorFilter
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (highlighted) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}
