package com.board2notes.app.presentation.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Rotate90DegreesCcw
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.board2notes.app.data.settings.OcrEngineChoice
import com.board2notes.app.domain.model.EnhancementMode
import com.board2notes.app.domain.model.FormattedNote
import com.board2notes.app.domain.model.PointF2
import com.board2notes.app.domain.model.Quad
import com.board2notes.app.domain.model.SavedNote
import com.board2notes.app.presentation.components.ImageWithQuad
import com.board2notes.app.presentation.components.PreviewImage
import com.board2notes.app.presentation.components.SectionTitle
import com.board2notes.app.presentation.components.StatusBanner
import com.board2notes.app.presentation.components.WarningText
import com.board2notes.app.presentation.state.Board2NotesUiState
import com.board2notes.app.presentation.state.Board2NotesViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private object Route {
    const val Home = "home"
    const val Capture = "capture"
    const val Review = "review"
    const val Crop = "crop"
    const val Enhancement = "enhancement"
    const val Ocr = "ocr"
    const val NoteEditor = "note_editor"
    const val Notes = "notes"
    const val NoteDetail = "note_detail"
    const val Settings = "settings"
    const val About = "about"
    const val Debug = "debug"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Board2NotesApp(viewModel: Board2NotesViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            viewModel.loadImage(it)
            navController.navigate(Route.Review)
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            pendingCameraUri?.let {
                viewModel.loadImage(it)
                navController.navigate(Route.Review)
            }
        }
    }

    LaunchedEffect(state.userMessage) {
        val message = state.userMessage
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route
    val topLevel = route in setOf(Route.Home, Route.Notes, Route.Settings)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    if (route == Route.Home) B2NoteLogo(compact = true) else Text(titleForRoute(route), fontWeight = FontWeight.SemiBold)
                },
                navigationIcon = {
                    if (!topLevel) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                        }
                    }
                },
                actions = {
                    if (state.settings.debugMode && state.note != null) {
                        IconButton(onClick = { navController.navigate(Route.Debug) }) {
                            Icon(Icons.Default.BugReport, contentDescription = "Debug")
                        }
                    }
                    if (route != Route.About) {
                        IconButton(onClick = { navController.navigate(Route.About) }) {
                            Icon(Icons.Default.Info, contentDescription = "Hakkinda")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (topLevel) {
                B2NoteBottomBar(route = route, navController = navController, viewModel = viewModel)
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (state.isBusy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            NavHost(navController = navController, startDestination = Route.Home) {
                composable(Route.Home) {
                    HomeScreen(
                        state = state,
                        onStart = { navController.navigate(Route.Capture) },
                        onOpenNote = { note ->
                            viewModel.openSavedNote(note.id)
                            navController.navigate("${Route.NoteDetail}/${note.id}")
                        }
                    )
                }
                composable(Route.Capture) {
                    CaptureScreen(
                        onPick = { galleryLauncher.launch("image/*") },
                        onCamera = {
                            val uri = createCameraUri(context)
                            pendingCameraUri = uri
                            cameraLauncher.launch(uri)
                        }
                    )
                }
                composable(Route.Review) {
                    ImageReviewScreen(
                        state = state,
                        viewModel = viewModel,
                        onContinue = {
                            viewModel.detectBoard()
                            navController.navigate(Route.Crop)
                        }
                    )
                }
                composable(Route.Crop) {
                    BoardCropScreen(
                        state = state,
                        viewModel = viewModel,
                        onContinue = {
                            viewModel.enhanceBoard()
                            navController.navigate(Route.Enhancement)
                        }
                    )
                }
                composable(Route.Enhancement) {
                    EnhancementResultScreen(
                        state = state,
                        viewModel = viewModel,
                        onRunOcr = {
                            viewModel.runOcrAndFormat()
                            navController.navigate(Route.Ocr)
                        }
                    )
                }
                composable(Route.Ocr) {
                    OcrReviewScreen(
                        state = state,
                        onOpenNote = { navController.navigate(Route.NoteEditor) }
                    )
                }
                composable(Route.NoteEditor) {
                    NoteEditorScreen(
                        state = state,
                        viewModel = viewModel,
                        onNotes = {
                            viewModel.refreshNotes()
                            navController.navigate(Route.Notes)
                        }
                    )
                }
                composable(Route.Notes) {
                    LaunchedEffect(Unit) { viewModel.refreshNotes() }
                    NotesScreen(
                        state = state,
                        onOpen = { note ->
                            viewModel.openSavedNote(note.id)
                            navController.navigate("${Route.NoteDetail}/${note.id}")
                        }
                    )
                }
                composable("${Route.NoteDetail}/{id}") { entry ->
                    val id = entry.arguments?.getString("id").orEmpty()
                    LaunchedEffect(id) { viewModel.openSavedNote(id) }
                    NoteDetailScreen(
                        state = state,
                        viewModel = viewModel,
                        onEdit = { navController.navigate(Route.NoteEditor) },
                        onDeleted = {
                            viewModel.deleteSelectedNote()
                            navController.navigate(Route.Notes)
                        }
                    )
                }
                composable(Route.Settings) {
                    SettingsScreen(
                        state = state,
                        viewModel = viewModel,
                        onAbout = { navController.navigate(Route.About) }
                    )
                }
                composable(Route.About) {
                    AboutScreen()
                }
                composable(Route.Debug) {
                    DebugArtifactsScreen(state, viewModel)
                }
            }
        }
    }
}

@Composable
private fun B2NoteBottomBar(route: String?, navController: NavHostController, viewModel: Board2NotesViewModel) {
    NavigationBar {
        NavigationBarItem(
            selected = route == Route.Home,
            onClick = {
                viewModel.goHome()
                navController.navigate(Route.Home)
            },
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text("Ana Sayfa") }
        )
        NavigationBarItem(
            selected = route == Route.Notes,
            onClick = {
                viewModel.refreshNotes()
                navController.navigate(Route.Notes)
            },
            icon = { Icon(Icons.Default.Folder, contentDescription = null) },
            label = { Text("Notlarim") }
        )
        NavigationBarItem(
            selected = route == Route.Settings,
            onClick = { navController.navigate(Route.Settings) },
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            label = { Text("Ayarlar") }
        )
    }
}

@Composable
private fun HomeScreen(state: Board2NotesUiState, onStart: () -> Unit, onOpenNote: (SavedNote) -> Unit) {
    ScreenColumn {
        Spacer(Modifier.height(18.dp))
        B2NoteLogo(compact = false)
        Spacer(Modifier.height(18.dp))
        SectionTitle(
            title = "Tahtadan temiz ders notuna",
            subtitle = "Fotoğraf çek, tahta alanını düzelt, yazıyı belirginleştir ve notlarını kalıcı olarak sakla."
        )
        Spacer(Modifier.height(18.dp))
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.CameraAlt, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Yeni tahta notu oluştur")
        }
        Spacer(Modifier.height(18.dp))
        PipelineSummary()
        Spacer(Modifier.height(20.dp))
        SectionTitle("Son notlar", "En son kaydedilen ders notların.")
        Spacer(Modifier.height(10.dp))
        if (state.savedNotes.isEmpty()) {
            EmptyState("Henüz kayıtlı not yok.", "İlk tahta fotoğrafını işleyerek B2Note arşivini oluştur.")
        } else {
            state.savedNotes.take(3).forEach { note ->
                SavedNoteRow(note = note, onClick = { onOpenNote(note) })
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun CaptureScreen(onPick: () -> Unit, onCamera: () -> Unit) {
    ScreenColumn {
        Spacer(Modifier.height(16.dp))
        PipelineStepper(current = 0)
        Spacer(Modifier.height(18.dp))
        SectionTitle("Görüntü kaynağı", "Tahta veya projeksiyon görüntüsünü kamera ya da galeriden al.")
        Spacer(Modifier.height(18.dp))
        Button(onClick = onCamera, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.CameraAlt, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Kamera ile çek")
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = onPick, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.PhotoLibrary, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Galeriden seç")
        }
        Spacer(Modifier.height(18.dp))
        EmptyState("İpucu", "Kadrajda tahtanın dört köşesini mümkün olduğunca görünür tut.")
    }
}

@Composable
private fun ImageReviewScreen(state: Board2NotesUiState, viewModel: Board2NotesViewModel, onContinue: () -> Unit) {
    ScreenColumn {
        Spacer(Modifier.height(16.dp))
        PipelineStepper(current = 0)
        Spacer(Modifier.height(16.dp))
        SectionTitle("Görüntüyü kontrol et", "Gerekirse döndür, sonra tahta alanını bul.")
        Spacer(Modifier.height(16.dp))
        state.selectedImage?.let { PreviewImage(it) } ?: StatusBanner("Görüntü yükleniyor...", busy = true)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { viewModel.rotateSelected(-90f) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Rotate90DegreesCcw, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Döndür")
            }
            Button(onClick = onContinue, modifier = Modifier.weight(1f), enabled = state.selectedImage != null) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Onayla")
            }
        }
    }
}

@Composable
private fun BoardCropScreen(state: Board2NotesUiState, viewModel: Board2NotesViewModel, onContinue: () -> Unit) {
    val detection = state.boardDetection
    ScreenColumn {
        Spacer(Modifier.height(16.dp))
        PipelineStepper(current = 1)
        Spacer(Modifier.height(16.dp))
        SectionTitle("Tahta alanı", "Overlay, crop ve köşe düzeltmesini kontrol et.")
        Spacer(Modifier.height(12.dp))
        if (state.isBusy) StatusBanner("Tahta alanı bulunuyor...", busy = true)
        detection?.warnings?.forEach { WarningText(it); Spacer(Modifier.height(8.dp)) }
        detection?.let {
            ImageWithQuad(bitmap = it.overlayBitmap, quad = state.manualQuad)
            Spacer(Modifier.height(16.dp))
            Text("Hassasiyet: ${"%.2f".format(state.settings.threshold)}")
            Slider(
                value = state.settings.threshold,
                onValueChange = viewModel::setThreshold,
                valueRange = 0.45f..0.60f
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = viewModel::detectBoard, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Tekrar")
                }
                Button(onClick = onContinue, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Devam")
                }
            }
            Spacer(Modifier.height(16.dp))
            SectionTitle("Düzeltilmiş crop")
            Spacer(Modifier.height(8.dp))
            PreviewImage(it.cropBitmap)
            Spacer(Modifier.height(16.dp))
            ManualCornerEditor(state, viewModel)
        }
    }
}

@Composable
private fun ManualCornerEditor(state: Board2NotesUiState, viewModel: Board2NotesViewModel) {
    val image = state.boardDetection?.originalImage ?: return
    val quad = state.manualQuad ?: return
    val points = listOf(quad.topLeft, quad.topRight, quad.bottomRight, quad.bottomLeft)
    val labels = listOf("Sol üst", "Sağ üst", "Sağ alt", "Sol alt")
    SectionTitle("Elle düzelt", "Köşeleri ince ayarla ve crop sonucunu yenile.")
    Spacer(Modifier.height(8.dp))
    labels.forEachIndexed { index, label ->
        Text(label, style = MaterialTheme.typography.labelLarge)
        CornerSlider("X", points[index].x, 0f..image.width.toFloat()) {
            viewModel.updateManualCorner(index, it, points[index].y)
        }
        CornerSlider("Y", points[index].y, 0f..image.height.toFloat()) {
            viewModel.updateManualCorner(index, points[index].x, it)
        }
        Spacer(Modifier.height(6.dp))
    }
    OutlinedButton(onClick = viewModel::applyManualPerspective, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.Tune, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Tahtayı düzelt")
    }
}

@Composable
private fun EnhancementResultScreen(state: Board2NotesUiState, viewModel: Board2NotesViewModel, onRunOcr: () -> Unit) {
    val enhancement = state.enhancement
    var tab by remember { mutableIntStateOf(0) }
    ScreenColumn {
        Spacer(Modifier.height(16.dp))
        PipelineStepper(current = 2)
        Spacer(Modifier.height(16.dp))
        SectionTitle("İyileştirme sonucu", "Model 2 iki çıktı üretir: OCR görüntüsü ve beyaz sayfa.")
        Spacer(Modifier.height(12.dp))
        if (state.isBusy) StatusBanner("Yazı belirginleştiriliyor...", busy = true)
        enhancement?.warnings?.forEach { WarningText(it); Spacer(Modifier.height(8.dp)) }
        enhancement?.let {
            Text("Orijinal crop", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            PreviewImage(it.inputBitmap)
            Spacer(Modifier.height(14.dp))
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("OCR Görüntüsü") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Beyaz Sayfa") })
            }
            Spacer(Modifier.height(10.dp))
            if (tab == 0) {
                PreviewImage(it.ocrCandidateBitmap)
            } else {
                PreviewImage(it.textLayerBitmap ?: it.ocrCandidateBitmap)
            }
            if (state.settings.debugMode) {
                Spacer(Modifier.height(10.dp))
                Text("Süre: ${it.elapsedMs} ms", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRunOcr, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("OCR ile metne çevir")
            }
        }
    }
}

@Composable
private fun OcrReviewScreen(state: Board2NotesUiState, onOpenNote: () -> Unit) {
    ScreenColumn {
        Spacer(Modifier.height(16.dp))
        PipelineStepper(current = 3)
        Spacer(Modifier.height(16.dp))
        SectionTitle("OCR sonucu", "Metin çıkarımı tamamlanınca not editörü açılabilir.")
        Spacer(Modifier.height(12.dp))
        if (state.isBusy) StatusBanner("ML Kit Latin OCR çalışıyor...", busy = true)
        state.ocrResult?.warnings?.forEach { WarningText(it); Spacer(Modifier.height(8.dp)) }
        state.ocrResult?.let { result ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(result.rawText.ifBlank { "OCR sonucu boş." }, modifier = Modifier.padding(12.dp))
            }
            Spacer(Modifier.height(14.dp))
            Button(onClick = onOpenNote, modifier = Modifier.fillMaxWidth(), enabled = state.note != null) {
                Icon(Icons.Default.Edit, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Notu aç")
            }
        }
    }
}

@Composable
private fun NoteEditorScreen(state: Board2NotesUiState, viewModel: Board2NotesViewModel, onNotes: () -> Unit) {
    val note = state.note ?: return
    val context = LocalContext.current
    var title by remember(note.createdAtEpochMs, state.activeSavedNoteId) { mutableStateOf(note.title) }
    var course by remember(note.createdAtEpochMs, state.activeSavedNoteId) { mutableStateOf(note.courseName) }
    var body by remember(note.createdAtEpochMs, state.activeSavedNoteId) { mutableStateOf(note.body) }
    ScreenColumn {
        Spacer(Modifier.height(16.dp))
        PipelineStepper(current = 4)
        Spacer(Modifier.height(16.dp))
        SectionTitle("Not editörü", "Ders notunu düzenle, kaydet ve paylaş.")
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(title, { title = it; viewModel.updateNote(it, body, course) }, label = { Text("Başlık") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(course, { course = it; viewModel.updateNote(title, body, it) }, label = { Text("Ders adı") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = body,
            onValueChange = { body = it; viewModel.updateNote(title, it, course) },
            label = { Text("Not") },
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
        )
        Spacer(Modifier.height(12.dp))
        state.enhancement?.textLayerBitmap?.let {
            SectionTitle("Beyaz sayfa çıktısı")
            Spacer(Modifier.height(8.dp))
            PreviewImage(it)
            Spacer(Modifier.height(12.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { copyNote(context, state.note ?: note) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.ContentCopy, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Kopyala")
            }
            Button(onClick = { viewModel.saveCurrentNote() }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Kaydet")
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { shareNote(context, state.note ?: note) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Paylaş")
            }
            OutlinedButton(onClick = onNotes, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Folder, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Notlarım")
            }
        }
    }
}

@Composable
private fun NotesScreen(state: Board2NotesUiState, onOpen: (SavedNote) -> Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(state.savedNotes, query) {
        val q = query.trim()
        if (q.isBlank()) state.savedNotes else state.savedNotes.filter {
            it.title.contains(q, true) || it.body.contains(q, true) || it.courseName.contains(q, true)
        }
    }
    ScreenColumn {
        Spacer(Modifier.height(16.dp))
        SectionTitle("Notlarım", "Kaydedilen tahta notların ve beyaz sayfa çıktıları.")
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Not ara") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(14.dp))
        if (filtered.isEmpty()) {
            EmptyState("Sonuç yok", "Yeni bir tahta notu oluştur veya arama metnini değiştir.")
        } else {
            filtered.forEach { note ->
                SavedNoteRow(note = note, onClick = { onOpen(note) })
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun NoteDetailScreen(
    state: Board2NotesUiState,
    viewModel: Board2NotesViewModel,
    onEdit: () -> Unit,
    onDeleted: () -> Unit
) {
    val note = state.selectedSavedNote
    val context = LocalContext.current
    ScreenColumn {
        Spacer(Modifier.height(16.dp))
        if (note == null) {
            StatusBanner("Not yükleniyor...", busy = true)
            return@ScreenColumn
        }
        SectionTitle(note.title, note.courseName.ifBlank { "Kaydedilen B2Note notu" })
        Spacer(Modifier.height(10.dp))
        Text(formatDate(note.updatedAtEpochMs), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        FilePreview(note.whitePageImagePath, "Beyaz sayfa")
        Spacer(Modifier.height(12.dp))
        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
            Text(note.body, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { copySavedNote(context, note) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.ContentCopy, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Kopyala")
            }
            Button(onClick = { shareSavedNote(context, note) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Paylaş")
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Edit, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Düzenle")
            }
            OutlinedButton(onClick = onDeleted, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Sil")
            }
        }
        if (state.settings.debugMode) {
            Spacer(Modifier.height(12.dp))
            FilePreview(note.cropImagePath, "Crop")
            Spacer(Modifier.height(8.dp))
            FilePreview(note.ocrImagePath, "OCR görüntüsü")
            Spacer(Modifier.height(8.dp))
            Text("OCR ham metni:\n${note.ocrText}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SettingsScreen(state: Board2NotesUiState, viewModel: Board2NotesViewModel, onAbout: () -> Unit) {
    ScreenColumn {
        Spacer(Modifier.height(16.dp))
        SectionTitle("Ayarlar", "Model davranışı, not üretimi ve geliştirici seçenekleri.")
        Spacer(Modifier.height(16.dp))
        Text("Tahta hassasiyeti: ${"%.2f".format(state.settings.threshold)}")
        Slider(state.settings.threshold, viewModel::setThreshold, valueRange = 0.45f..0.60f)
        ToggleRow("Debug mode", state.settings.debugMode, viewModel::setDebugMode)
        ToggleRow("LLM ile düzenleme", state.settings.llmEnabled, viewModel::setLlmEnabled)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = state.settings.groqApiKey,
            onValueChange = viewModel::setGroqApiKey,
            label = { Text("Groq API anahtarı") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(16.dp))
        Text("Model 2 önceliği", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            ModeButton("OCR", state.settings.enhancementMode == EnhancementMode.Ocr, Modifier.weight(1f)) {
                viewModel.setEnhancementMode(EnhancementMode.Ocr)
            }
            ModeButton("Beyaz sayfa", state.settings.enhancementMode == EnhancementMode.VisualNote, Modifier.weight(1f)) {
                viewModel.setEnhancementMode(EnhancementMode.VisualNote)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("OCR motoru", style = MaterialTheme.typography.titleMedium)
        OcrEngineChoice.entries.forEach { choice ->
            TextButton(onClick = { viewModel.setOcrEngineChoice(choice) }, modifier = Modifier.fillMaxWidth()) {
                Text(if (state.settings.ocrEngineChoice == choice) "✓ ${choice.name}" else choice.name)
            }
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = onAbout, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Info, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Hakkında")
        }
        WarningText("TrOCR ve PaddleOCR adapterleri ileride bağlanmak üzere ayrıldı; bu sürümde birincil OCR ML Kit Latin kullanır.")
    }
}

@Composable
private fun AboutScreen() {
    ScreenColumn {
        Spacer(Modifier.height(24.dp))
        B2NoteLogo(compact = false)
        Spacer(Modifier.height(18.dp))
        SectionTitle("B2Note hakkında", "Tahta görüntülerini düzenlenebilir ders notlarına dönüştüren yerel-öncelikli Android uygulaması.")
        Spacer(Modifier.height(14.dp))
        InfoLine("Sürüm", "0.1.0 ürün prototipi")
        InfoLine("Model 1", "ONNX tahta/projeksiyon segmentasyonu")
        InfoLine("Model 2", "ONNX iyileştirme, OCR görüntüsü ve beyaz sayfa çıktısı")
        InfoLine("OCR", "ML Kit Text Recognition v2 Latin")
        InfoLine("Gizlilik", "Notlar ve görseller cihazın uygulama alanında saklanır.")
    }
}

@Composable
private fun DebugArtifactsScreen(state: Board2NotesUiState, viewModel: Board2NotesViewModel) {
    ScreenColumn {
        Spacer(Modifier.height(16.dp))
        SectionTitle("Debug çıktıları", "Aktif pipeline görselleri ve metinleri geçici klasöre kaydedilir.")
        Spacer(Modifier.height(12.dp))
        Button(onClick = viewModel::exportDebugArtifacts, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.BugReport, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Debug çıktılarını kaydet")
        }
        Spacer(Modifier.height(12.dp))
        state.debugFiles.forEach { file ->
            Text(file.absolutePath, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun B2NoteLogo(compact: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(if (compact) 34.dp else 62.dp)) {
            Canvas(modifier = Modifier.fillMaxSize().padding(7.dp)) {
                drawRoundRect(Color.White, topLeft = Offset(size.width * 0.12f, size.height * 0.08f), size = Size(size.width * 0.72f, size.height * 0.72f))
                drawLine(Color(0xFF0F5F66), Offset(size.width * 0.28f, size.height * 0.32f), Offset(size.width * 0.72f, size.height * 0.32f), strokeWidth = 3f)
                drawLine(Color(0xFF0F5F66), Offset(size.width * 0.28f, size.height * 0.48f), Offset(size.width * 0.62f, size.height * 0.48f), strokeWidth = 3f)
                drawLine(Color(0xFFF3B340), Offset(size.width * 0.54f, size.height * 0.72f), Offset(size.width * 0.88f, size.height * 0.96f), strokeWidth = 6f)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                "B2Note",
                fontFamily = FontFamily.Cursive,
                fontWeight = FontWeight.Bold,
                fontSize = if (compact) 24.sp else 38.sp,
                color = MaterialTheme.colorScheme.primary
            )
            if (!compact) Text("Board to notes", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PipelineStepper(current: Int) {
    val steps = listOf("Görüntü", "Tahta", "İyileştirme", "OCR", "Not")
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        steps.forEachIndexed { index, label ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Surface(
                    shape = CircleShape,
                    color = if (index <= current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("${index + 1}", color = if (index <= current) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(label, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun PipelineSummary() {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.padding(14.dp)) {
            Text("İşlem akışı", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text("1. Tahta alanı bulunur ve crop düzeltilir.")
            Text("2. Yazı netleştirilir ve beyaz sayfa çıktısı üretilir.")
            Text("3. OCR metni düzenlenebilir nota dönüştürülür.")
        }
    }
}

@Composable
private fun SavedNoteRow(note: SavedNote, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            FileThumbnail(note.whitePageImagePath)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(note.title, fontWeight = FontWeight.SemiBold)
                if (note.courseName.isNotBlank()) Text(note.courseName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                Text(note.preview.ifBlank { "Boş not" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatDate(note.updatedAtEpochMs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun FileThumbnail(path: String?) {
    val bitmap = remember(path) { path?.let { BitmapFactory.decodeFile(it) } }
    Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(58.dp)) {
        if (bitmap != null) {
            Image(bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize())
        } else {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Image, contentDescription = null)
            }
        }
    }
}

@Composable
private fun FilePreview(path: String?, label: String) {
    val bitmap = remember(path) { path?.let { BitmapFactory.decodeFile(it) } }
    if (bitmap != null) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        PreviewImage(bitmap)
    }
}

@Composable
private fun EmptyState(title: String, body: String) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.padding(14.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = FontWeight.SemiBold)
        Text(value, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Spacer(Modifier.height(10.dp))
    Divider()
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ModeButton(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    if (selected) Button(onClick = onClick, modifier = modifier) { Text(label) }
    else OutlinedButton(onClick = onClick, modifier = modifier) { Text(label) }
}

@Composable
private fun CornerSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("$label ${value.toInt()}", modifier = Modifier.width(64.dp), style = MaterialTheme.typography.bodySmall)
        Slider(value = value.coerceIn(range.start, range.endInclusive), onValueChange = onChange, valueRange = range)
    }
}

@Composable
private fun ScreenColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        content = content
    )
}

private fun titleForRoute(route: String?): String = when {
    route == Route.Capture -> "Yeni not"
    route == Route.Review -> "Görüntü"
    route == Route.Crop -> "Tahta crop"
    route == Route.Enhancement -> "İyileştirme"
    route == Route.Ocr -> "OCR"
    route == Route.NoteEditor -> "Not editörü"
    route == Route.Notes -> "Notlarım"
    route?.startsWith(Route.NoteDetail) == true -> "Not detayı"
    route == Route.Settings -> "Ayarlar"
    route == Route.About -> "Hakkında"
    route == Route.Debug -> "Debug"
    else -> "B2Note"
}

private fun createCameraUri(context: Context): Uri {
    val dir = File(context.cacheDir, "camera").apply { mkdirs() }
    val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val file = File(dir, "b2note_$stamp.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

private fun copyNote(context: Context, note: FormattedNote) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(note.title, note.toShareText()))
}

private fun shareNote(context: Context, note: FormattedNote) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, note.title)
        putExtra(Intent.EXTRA_TEXT, note.toShareText())
    }
    context.startActivity(Intent.createChooser(intent, "Notu paylaş"))
}

private fun copySavedNote(context: Context, note: SavedNote) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(note.title, note.toShareText()))
}

private fun shareSavedNote(context: Context, note: SavedNote) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, note.title)
        putExtra(Intent.EXTRA_TEXT, note.toShareText())
    }
    context.startActivity(Intent.createChooser(intent, "Notu paylaş"))
}

private fun FormattedNote.toShareText(): String = buildString {
    appendLine(title)
    if (courseName.isNotBlank()) appendLine(courseName)
    appendLine()
    append(body)
}

private fun SavedNote.toShareText(): String = buildString {
    appendLine(title)
    if (courseName.isNotBlank()) appendLine(courseName)
    appendLine()
    append(body)
}

private fun formatDate(epochMs: Long): String =
    SimpleDateFormat("dd MMM yyyy HH:mm", Locale("tr", "TR")).format(Date(epochMs))
