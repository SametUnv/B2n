package com.board2notes.app.presentation.ocrnote


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.board2notes.app.core.ui.components.AppScaffold
import com.board2notes.app.core.ui.components.PrimaryButton
import com.board2notes.app.core.ui.components.SecondaryButton
import com.board2notes.app.core.util.ShareUtil
import com.board2notes.app.presentation.shared.BoardViewModel

@Composable
fun OcrNoteScreen(
    viewModel: BoardViewModel,
    onBack: () -> Unit,
    onExported: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    AppScaffold(title = "OCR Notu", onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Algılanan metin",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Metni serbestçe düzenleyebilirsin.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = state.mockOcrText,
                onValueChange = viewModel::updateOcrText,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 280.dp),
                shape = MaterialTheme.shapes.medium,
                textStyle = MaterialTheme.typography.bodyLarge,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            Spacer(Modifier.height(20.dp))

            PrimaryButton(
                text = "PDF Olarak Kaydet",
                onClick = { viewModel.exportCurrentNote(onExported = onExported) },
                leadingIcon = Icons.Default.PictureAsPdf
            )
            Spacer(Modifier.height(12.dp))
            SecondaryButton(
                text = "Paylaş",
                onClick = { ShareUtil.shareText(context, state.mockOcrText) },
                leadingIcon = Icons.Default.Share
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}
