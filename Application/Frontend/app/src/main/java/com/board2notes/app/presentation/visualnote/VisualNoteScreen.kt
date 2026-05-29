package com.board2notes.app.presentation.visualnote

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.board2notes.app.core.ui.components.AppScaffold
import com.board2notes.app.core.ui.components.PrimaryButton
import com.board2notes.app.core.ui.components.SecondaryButton
import com.board2notes.app.core.ui.theme.Outline
import com.board2notes.app.core.util.ShareUtil
import com.board2notes.app.presentation.shared.BoardViewModel

@Composable
fun VisualNoteScreen(
    viewModel: BoardViewModel,
    onBack: () -> Unit,
    onExported: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    AppScaffold(title = "Görsel Not", onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Temiz not sayfası",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(14.dp))

            // Notebook page
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Box {
                    // Ruled-line background
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((state.visualNoteLines.size * 44 + 64).dp)
                    ) {
                        val lineGap = 44.dp.toPx()
                        var y = 60.dp.toPx()
                        while (y < size.height) {
                            drawLine(
                                color = Outline,
                                start = Offset(24.dp.toPx(), y),
                                end = Offset(size.width - 24.dp.toPx(), y),
                                strokeWidth = 1f
                            )
                            y += lineGap
                        }
                        // left margin line
                        drawLine(
                            color = Color(0xFFFCA5A5),
                            start = Offset(56.dp.toPx(), 0f),
                            end = Offset(56.dp.toPx(), size.height),
                            strokeWidth = 2f
                        )
                    }

                    // Writing layer
                    Column(modifier = Modifier.padding(start = 64.dp, top = 24.dp, end = 20.dp, bottom = 20.dp)) {
                        Text(
                            text = state.visualNoteTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(16.dp))
                        state.visualNoteLines.forEach { line ->
                            Text(
                                text = line,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.height(44.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            PrimaryButton(
                text = "PDF Olarak Kaydet",
                onClick = { viewModel.exportCurrentNote(onExported = onExported) },
                leadingIcon = Icons.Default.PictureAsPdf
            )
            Spacer(Modifier.height(12.dp))
            SecondaryButton(
                text = "Görsel Olarak Kaydet",
                onClick = { viewModel.exportCurrentNote(onExported = onExported) },
                leadingIcon = Icons.Default.Image
            )
            Spacer(Modifier.height(12.dp))
            SecondaryButton(
                text = "Paylaş",
                onClick = {
                    val content = (listOf(state.visualNoteTitle) + state.visualNoteLines).joinToString("\n")
                    ShareUtil.shareText(context, content)
                },
                leadingIcon = Icons.Default.Share
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}
