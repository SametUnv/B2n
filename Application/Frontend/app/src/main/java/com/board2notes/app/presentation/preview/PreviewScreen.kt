package com.board2notes.app.presentation.preview

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Rotate90DegreesCcw
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.board2notes.app.core.ui.components.AppScaffold
import com.board2notes.app.core.ui.components.PrimaryButton
import com.board2notes.app.core.ui.components.SecondaryButton
import com.board2notes.app.core.util.ImageEditUtil
import com.board2notes.app.presentation.shared.BoardViewModel
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PreviewScreen(
    viewModel: BoardViewModel,
    onContinue: () -> Unit,
    onReselect: () -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Crop launcher — opens the cropper UI and updates the VM with the result.
    val cropLauncher = rememberLauncherForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            result.uriContent?.let { viewModel.onImageSelected(it.toString()) }
        }
    }

    AppScaffold(title = "Önizleme", onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Seçtiğin tahta görseli",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(MaterialTheme.shapes.large),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = state.selectedImageUri,
                    contentDescription = "Seçilen görsel",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(3f / 4f)
                        .clip(MaterialTheme.shapes.large),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(Modifier.height(16.dp))

            // Rotate / Crop — now actually functional.
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AssistChip(
                    onClick = {
                        val uriString = state.selectedImageUri ?: return@AssistChip
                        scope.launch {
                            val rotated = withContext(Dispatchers.IO) {
                                ImageEditUtil.rotate(context, Uri.parse(uriString), 90f)
                            }
                            rotated?.let { viewModel.onImageSelected(it.toString()) }
                        }
                    },
                    label = { Text("Döndür") },
                    leadingIcon = {
                        Icon(Icons.Default.Rotate90DegreesCcw, contentDescription = null)
                    },
                    colors = AssistChipDefaults.assistChipColors()
                )
                AssistChip(
                    onClick = {
                        val uriString = state.selectedImageUri ?: return@AssistChip
                        cropLauncher.launch(
                            CropImageContractOptions(
                                uri = Uri.parse(uriString),
                                cropImageOptions = CropImageOptions(
                                    guidelines = com.canhub.cropper.CropImageView.Guidelines.ON,
                                    fixAspectRatio = false,
                                    outputCompressFormat = android.graphics.Bitmap.CompressFormat.JPEG,
                                    outputCompressQuality = 92
                                )
                            )
                        )
                    },
                    label = { Text("Kırp") },
                    leadingIcon = { Icon(Icons.Default.Crop, contentDescription = null) }
                )
            }

            Spacer(Modifier.height(20.dp))

            PrimaryButton(text = "Devam Et", onClick = onContinue)
            Spacer(Modifier.height(12.dp))
            SecondaryButton(text = "Yeniden Seç", onClick = onReselect)
            Spacer(Modifier.height(8.dp))
        }
    }
}