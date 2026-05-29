package com.board2notes.app.presentation.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.board2notes.app.core.ui.components.AppScaffold
import com.board2notes.app.core.ui.components.InfoCard
import com.board2notes.app.core.ui.components.PrimaryButton
import com.board2notes.app.core.ui.components.SecondaryButton
import com.board2notes.app.core.util.ImageFileProvider
import com.board2notes.app.presentation.shared.BoardViewModel

@Composable
fun HomeScreen(
    viewModel: BoardViewModel,
    onImageReady: () -> Unit
) {
    val context = LocalContext.current

    // Holds the target Uri for the camera capture.
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    // Gallery picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onImageSelected(uri.toString())
            onImageReady()
        }
    }

    // Camera capture
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            viewModel.onImageSelected(uri.toString())
            onImageReady()
        }
    }

    fun launchCamera() {
        // TakePicture delegates to an external camera app, so no CAMERA
        // runtime permission is required here.
        val uri = ImageFileProvider.createImageUri(context)
        pendingCameraUri = uri
        cameraLauncher.launch(uri)
    }

    AppScaffold(title = "Board2Notes") {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Tahtanın fotoğrafını ekle",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Bir fotoğraf çek ya da galeriden seç; gerisini Board2Notes halletsin.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            PrimaryButton(
                text = "Kamera ile Çek",
                onClick = { launchCamera() },
                leadingIcon = Icons.Default.PhotoCamera
            )
            Spacer(Modifier.height(12.dp))
            SecondaryButton(
                text = "Galeriden Seç",
                onClick = { galleryLauncher.launch("image/*") },
                leadingIcon = Icons.Default.PhotoLibrary
            )

            Spacer(Modifier.height(28.dp))

            Text(
                text = "Board2Notes ne yapar?",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoCard(
                    icon = Icons.Default.WbSunny,
                    title = "Görüntüyü temizle",
                    subtitle = "Parlama ve gölgeleri azaltır, tahtayı okunur hale getirir."
                )
                InfoCard(
                    icon = Icons.Default.TextFields,
                    title = "Yazıları algıla",
                    subtitle = "Tahtadaki metni dijital, düzenlenebilir nota çevirir."
                )
                InfoCard(
                    icon = Icons.Default.AutoFixHigh,
                    title = "Not oluştur",
                    subtitle = "OCR notu ya da temiz bir görsel not sayfası üretir."
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
