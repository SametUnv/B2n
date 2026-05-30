package com.board2notes.app.presentation.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Rotate90DegreesCcw
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
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
import kotlin.math.abs
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Brush

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
    const val Folder = "folder"
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
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val uri = createCameraUri(context)
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            viewModel.showMessage("Kamera izni olmadan fotoğraf çekilemez.")
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
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    shape = RoundedCornerShape(16.dp),
                    containerColor = Color(0xFF0F172A),
                    contentColor = Color.White,
                    actionColor = Color(0xFF2563EB),
                    actionContentColor = Color(0xFF2563EB),
                    dismissActionContentColor = Color.White
                )
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        titleForRoute(route),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
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
                    if (topLevel) {
                        IconButton(onClick = { navController.navigate(Route.About) }) {
                            Icon(Icons.Default.Info, contentDescription = "Hakkında")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                .then(
                    if (topLevel) {
                        Modifier.topLevelSwipeNavigation(
                            route = route,
                            navController = navController,
                            viewModel = viewModel
                        )
                    } else {
                        Modifier
                    }
                )
        ) {
            if (state.isBusy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            NavHost(
                navController = navController,
                startDestination = Route.Home,
                enterTransition = { topLevelEnterTransition(initialState.destination.route, targetState.destination.route) },
                exitTransition = { topLevelExitTransition(initialState.destination.route, targetState.destination.route) },
                popEnterTransition = { topLevelEnterTransition(initialState.destination.route, targetState.destination.route) },
                popExitTransition = { topLevelExitTransition(initialState.destination.route, targetState.destination.route) }
            ) {
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
                            val permissionGranted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                            if (permissionGranted) {
                                val uri = createCameraUri(context)
                                pendingCameraUri = uri
                                cameraLauncher.launch(uri)
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
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
                        },
                        onSkipToNote = {
                            viewModel.skipToNoteEditor()
                            navController.navigate(Route.NoteEditor)
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
                        },
                        onFolderOpen = { course ->
                            navController.navigate("${Route.Folder}/${Uri.encode(course)}")
                        }
                    )
                }
                composable("${Route.Folder}/{course}") { entry ->
                    val course = entry.arguments?.getString("course")?.let { Uri.decode(it) }.orEmpty()
                    LaunchedEffect(Unit) { viewModel.refreshNotes() }
                    FolderScreen(
                        state = state,
                        viewModel = viewModel,
                        courseName = course,
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
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            NavigationBarItem(
                selected = route == Route.Home,
                onClick = { navigateTopLevel(Route.Home, navController, viewModel) },
                icon = { Icon(Icons.Default.Home, contentDescription = null) },
                label = { Text("Ana Sayfa", fontWeight = if (route == Route.Home) FontWeight.SemiBold else FontWeight.Normal) },
                colors = b2NoteNavigationItemColors()
            )
            NavigationBarItem(
                selected = route == Route.Notes,
                onClick = { navigateTopLevel(Route.Notes, navController, viewModel) },
                icon = { Icon(Icons.Default.Folder, contentDescription = null) },
                label = { Text("Notlarım", fontWeight = if (route == Route.Notes) FontWeight.SemiBold else FontWeight.Normal) },
                colors = b2NoteNavigationItemColors()
            )
            NavigationBarItem(
                selected = route == Route.Settings,
                onClick = { navigateTopLevel(Route.Settings, navController, viewModel) },
                icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                label = { Text("Ayarlar", fontWeight = if (route == Route.Settings) FontWeight.SemiBold else FontWeight.Normal) },
                colors = b2NoteNavigationItemColors()
            )
        }
    }
}

@Composable
private fun b2NoteNavigationItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = MaterialTheme.colorScheme.primary,
    selectedTextColor = MaterialTheme.colorScheme.primary,
    indicatorColor = Color(0xFFEFF6FF),
    unselectedIconColor = Color(0xFF64748B),
    unselectedTextColor = Color(0xFF64748B)
)

private fun navigateTopLevel(route: String, navController: NavHostController, viewModel: Board2NotesViewModel) {
    when (route) {
        Route.Home -> viewModel.goHome()
        Route.Notes -> viewModel.refreshNotes()
    }
    navController.navigate(route) {
        popUpTo(Route.Home) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun Modifier.topLevelSwipeNavigation(
    route: String?,
    navController: NavHostController,
    viewModel: Board2NotesViewModel
): Modifier = pointerInput(route) {
    var totalDrag = 0f
    detectHorizontalDragGestures(
        onDragStart = { totalDrag = 0f },
        onHorizontalDrag = { _, dragAmount -> totalDrag += dragAmount },
        onDragEnd = {
            if (abs(totalDrag) < 92f) return@detectHorizontalDragGestures
            val targetRoute = if (totalDrag < 0f) nextTopLevelRoute(route) else previousTopLevelRoute(route)
            navigateTopLevel(targetRoute, navController, viewModel)
        }
    )
}

private fun nextTopLevelRoute(route: String?): String = when (route) {
    Route.Home -> Route.Notes
    Route.Notes -> Route.Settings
    Route.Settings -> Route.Home
    else -> Route.Home
}

private fun previousTopLevelRoute(route: String?): String = when (route) {
    Route.Home -> Route.Settings
    Route.Notes -> Route.Home
    Route.Settings -> Route.Notes
    else -> Route.Home
}

private fun AnimatedContentTransitionScope<*>.topLevelEnterTransition(fromRoute: String?, toRoute: String?): EnterTransition {
    val direction = if (nextTopLevelRoute(fromRoute) == toRoute) {
        AnimatedContentTransitionScope.SlideDirection.Left
    } else {
        AnimatedContentTransitionScope.SlideDirection.Right
    }
    return slideIntoContainer(direction, animationSpec = tween(260)) + fadeIn(tween(180))
}

private fun AnimatedContentTransitionScope<*>.topLevelExitTransition(fromRoute: String?, toRoute: String?): ExitTransition {
    val direction = if (nextTopLevelRoute(fromRoute) == toRoute) {
        AnimatedContentTransitionScope.SlideDirection.Left
    } else {
        AnimatedContentTransitionScope.SlideDirection.Right
    }
    return slideOutOfContainer(direction, animationSpec = tween(260)) + fadeOut(tween(180))
}

@Composable
private fun HomeScreen(state: Board2NotesUiState, onStart: () -> Unit, onOpenNote: (SavedNote) -> Unit) {
    ScreenColumn {
        Spacer(Modifier.height(10.dp))
        HomeWelcomeCard()
        Spacer(Modifier.height(18.dp))
        HomePrimaryActionCard(onClick = onStart)
        Spacer(Modifier.height(24.dp))
        Text(
            "Son Notlar",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(12.dp))
        if (state.savedNotes.isEmpty()) {
            HomeEmptyNotesCard()
        } else {
            HomeRecentNotesCard {
                state.savedNotes.take(3).forEachIndexed { index, note ->
                    HomeRecentNoteRow(note = note, onClick = { onOpenNote(note) })
                    if (index != state.savedNotes.take(3).lastIndex) {
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(22.dp))
    }
}

@Composable
private fun HomeWelcomeCard() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(20.dp), ambientColor = Color(0x142563EB), spotColor = Color(0x1A0F172A)),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF0F172A), Color(0xFF1D4ED8), Color(0xFF2563EB))
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color.White.copy(alpha = 0.14f)
                ) {
                    Text(
                        "AI destekli üretkenlik",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    "Tahta fotoğraflarınızı akıllı notlara dönüştürün.",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Ders içeriklerini yakalayın, düzenleyin ve not arşivinize kaydedin.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.78f)
                )
            }
        }
    }
}

@Composable
private fun HomePrimaryActionCard(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(188.dp)
            .shadow(16.dp, RoundedCornerShape(24.dp), ambientColor = Color(0x242563EB), spotColor = Color(0x302563EB))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF2563EB), Color(0xFF1D4ED8), Color(0xFF0F172A))
                    )
                )
                .padding(22.dp)
        ) {
            Column(
                modifier = Modifier.align(Alignment.CenterStart),
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    modifier = Modifier.size(54.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White.copy(alpha = 0.16f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "Yeni Tahta Notu Oluştur",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Kamera veya galeriden başlayın",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.78f)
                )
            }
            Icon(
                Icons.Default.Description,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.14f),
                modifier = Modifier
                    .size(104.dp)
                    .align(Alignment.BottomEnd)
            )
        }
    }
}

@Composable
private fun HomeRecentNotesCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(Modifier.padding(12.dp), content = content)
    }
}

@Composable
private fun HomeRecentNoteRow(note: SavedNote, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFFFFFFF)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    note.title.ifBlank { "Başlıksız not" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    formatDate(note.createdAtEpochMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    note.preview.ifBlank { "Önizleme bulunmuyor." },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(12.dp))
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeEmptyNotesCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(58.dp),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "Henüz not oluşturulmadı",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "İlk tahta notunuzu oluşturarak başlayın.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CaptureScreen(onPick: () -> Unit, onCamera: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 18.dp)
    ) {
        Spacer(Modifier.height(14.dp))
        Text(
            "Tahta yakala",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Tahtayı kadraja alın; köşeler görünür olduğunda en iyi sonuç alınır.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))
        CapturePreviewCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalButton(
                onClick = onPick,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Galeri")
            }
            Spacer(Modifier.width(18.dp))
            Button(
                onClick = onCamera,
                modifier = Modifier
                    .weight(1f)
                    .height(58.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text("Çek", fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun ImageReviewScreen(state: Board2NotesUiState, viewModel: Board2NotesViewModel, onContinue: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            PipelineStepper(current = 0)
            Spacer(Modifier.height(16.dp))
            SectionTitle("Görüntüyü kontrol et", "Gerekirse döndür, sonra devam et.")
            Spacer(Modifier.height(16.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                shadowElevation = 2.dp,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    state.selectedImage?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "Seçilen görüntü",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } ?: StatusBanner("Görüntü yükleniyor...", busy = true)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            shadowElevation = 0.dp
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.rotateSelected(-90f) },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                Icon(Icons.Default.Rotate90DegreesCcw, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                Text("Döndür")
            }
                Button(
                    onClick = onContinue,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    enabled = state.selectedImage != null,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Devam Et")
                }
            }
        }
    }
}

@Composable
private fun CapturePreviewCard(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = Color(0xFF0F172A),
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF0F172A), Color(0xFF172554), Color(0xFF1E3A8A))
                    )
                )
                .padding(24.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 4.dp.toPx()
                val line = 42.dp.toPx()
                val corner = 14.dp.toPx()
                val scanY = size.height * 0.44f
                drawLine(
                    color = Color(0x662563EB),
                    start = Offset(size.width * 0.08f, scanY),
                    end = Offset(size.width * 0.92f, scanY),
                    strokeWidth = 2.dp.toPx()
                )
                listOf(
                    Offset(0f, 0f),
                    Offset(size.width, 0f),
                    Offset(0f, size.height),
                    Offset(size.width, size.height)
                ).forEach { point ->
                    val left = point.x == 0f
                    val top = point.y == 0f
                    val x = if (left) corner else size.width - corner
                    val y = if (top) corner else size.height - corner
                    val horizontalEnd = if (left) x + line else x - line
                    val verticalEnd = if (top) y + line else y - line
                    drawLine(Color.White.copy(alpha = 0.82f), Offset(x, y), Offset(horizontalEnd, y), stroke)
                    drawLine(Color.White.copy(alpha = 0.82f), Offset(x, y), Offset(x, verticalEnd), stroke)
                }
            }
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.size(76.dp),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "Akıllı tahta algılama",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Köşeleri hizalayın, Board2Note notunuzu hazırlasın.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.72f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun BoardCropScreen(
    state: Board2NotesUiState,
    viewModel: Board2NotesViewModel,
    onContinue: () -> Unit,
    onSkipToNote: () -> Unit = {}
) {
    val detection = state.boardDetection
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(18.dp))
        PipelineStepper(current = 1)
        Spacer(Modifier.height(18.dp))
        BoardCropHeader()
        Spacer(Modifier.height(18.dp))
        if (state.isBusy) {
            StatusBanner("Tahta alanı bulunuyor...", busy = true)
            Spacer(Modifier.height(12.dp))
        }
        detection?.warnings?.filterNot(::isManualCropAppliedWarning)?.forEach {
            WarningText(it)
            Spacer(Modifier.height(8.dp))
        }
        detection?.let {
            CropPreviewPanel {
                ImageWithQuad(
                    bitmap = it.overlayBitmap,
                    quad = state.manualQuad,
                    onCornerDrag = viewModel::updateManualCorner
                )
            }
            Spacer(Modifier.height(14.dp))
            CropThresholdPanel(
                threshold = state.settings.threshold,
                onThresholdChange = viewModel::setThreshold
            )
            Spacer(Modifier.height(14.dp))
            CropActionPanel(
                onRetry = viewModel::detectBoard,
                onContinue = onContinue,
                onSkipToNote = onSkipToNote
            )
            Spacer(Modifier.height(18.dp))
            CropResultPanel {
                PreviewImage(it.cropBitmap)
            }
            Spacer(Modifier.height(16.dp))
            ManualCornerEditor(state, viewModel)
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun BoardCropHeader() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Tahta Alanını Seç",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Tahtanın köşelerini kontrol edin ve gerekli düzenlemeleri yapın.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun isManualCropAppliedWarning(message: String): Boolean =
    message.equals("Elle düzeltilmiş kırpma uygulandı.", ignoreCase = true) ||
        message.equals("Elle duzeltilmis kirpma uygulandi.", ignoreCase = true)

@Composable
private fun CropPreviewPanel(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Algılanan alan",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color(0xFFEFF6FF)
                ) {
                    Text(
                        "Köşe kontrolü",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Box(Modifier.padding(8.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun CropThresholdPanel(threshold: Float, onThresholdChange: (Float) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Algılama hassasiyeti",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "%.2f".format(threshold),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = threshold,
                onValueChange = onThresholdChange,
                valueRange = 0.45f..0.60f
            )
        }
    }
}

@Composable
private fun CropActionPanel(
    onRetry: () -> Unit,
    onContinue: () -> Unit,
    onSkipToNote: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onRetry,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("Tekrar")
                }
                Button(
                    onClick = onContinue,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("Devam", fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onSkipToNote,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1))
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("İşlemi atla → Not Editörü")
            }
        }
    }
}

@Composable
private fun CropResultPanel(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                "Düzeltilmiş crop",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun ManualCornerEditor(state: Board2NotesUiState, viewModel: Board2NotesViewModel) {
    val image = state.boardDetection?.originalImage ?: return
    val quad = state.manualQuad ?: return
    val points = listOf(quad.topLeft, quad.topRight, quad.bottomRight, quad.bottomLeft)
    val labels = listOf("Sol üst", "Sağ üst", "Sağ alt", "Sol alt")
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Elle düzelt",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Köşeleri ince ayarla ve crop sonucunu yenile.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(14.dp))
            labels.forEachIndexed { index, label ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.height(8.dp))
                        CornerSlider("X", points[index].x, 0f..image.width.toFloat()) {
                            viewModel.updateManualCorner(index, it, points[index].y)
                        }
                        CornerSlider("Y", points[index].y, 0f..image.height.toFloat()) {
                            viewModel.updateManualCorner(index, points[index].x, it)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
            OutlinedButton(
                onClick = viewModel::applyManualPerspective,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1))
            ) {
                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Tahtayı düzelt", fontWeight = FontWeight.SemiBold)
            }
        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteEditorScreen(state: Board2NotesUiState, viewModel: Board2NotesViewModel, onNotes: () -> Unit) {
    val note = state.note ?: return
    val context = LocalContext.current
    var title by remember(note.createdAtEpochMs, state.activeSavedNoteId) { mutableStateOf(note.title) }
    var course by remember(note.createdAtEpochMs, state.activeSavedNoteId) { mutableStateOf(note.courseName) }
    var body by remember(note.createdAtEpochMs, state.activeSavedNoteId) { mutableStateOf(note.body) }
    val wordCount = body.split("\\s+".toRegex()).count { it.isNotBlank() }
    val existingCourses = remember(state.savedNotes) {
        state.savedNotes.map { it.courseName }.filter { it.isNotBlank() }.distinct().sorted()
    }
    var courseDropdownExpanded by remember { mutableStateOf(false) }

    ScreenColumn {
        Spacer(Modifier.height(20.dp))
        PipelineStepper(current = 4)
        Spacer(Modifier.height(18.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            shadowElevation = 2.dp,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Not Editörü",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            "Düzenle, kaydet ve paylaş",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            "$wordCount kelime",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.TextFields,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Başlık ve Ders",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; viewModel.updateNote(it, body, course) },
                    label = { Text("Başlık") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(Modifier.height(12.dp))
                ExposedDropdownMenuBox(
                    expanded = courseDropdownExpanded && existingCourses.isNotEmpty(),
                    onExpandedChange = {
                        if (existingCourses.isNotEmpty()) courseDropdownExpanded = it
                    }
                ) {
                    OutlinedTextField(
                        value = course,
                        onValueChange = {
                            course = it
                            viewModel.updateNote(title, body, it)
                            courseDropdownExpanded = it.isNotEmpty() && existingCourses.any { c ->
                                c.contains(it, ignoreCase = true) && !c.equals(it, ignoreCase = true)
                            }
                        },
                        label = { Text("Ders adı") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        trailingIcon = {
                            if (existingCourses.isNotEmpty()) {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = courseDropdownExpanded)
                            }
                        }
                    )
                    val filtered = if (course.isBlank()) existingCourses
                        else existingCourses.filter { it.contains(course, ignoreCase = true) }
                    if (filtered.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = courseDropdownExpanded,
                            onDismissRequest = { courseDropdownExpanded = false }
                        ) {
                            filtered.forEach { courseName ->
                                DropdownMenuItem(
                                    text = { Text(courseName) },
                                    onClick = {
                                        course = courseName
                                        viewModel.updateNote(title, body, courseName)
                                        courseDropdownExpanded = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            shadowElevation = 2.dp,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Not İçeriği",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFF1F5F9)
                    ) {
                        Text(
                            "Canlı düzenleme",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it; viewModel.updateNote(title, it, course) },
                    placeholder = { Text("Notlarınızı buraya yazın...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp),
                    shape = RoundedCornerShape(14.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        state.enhancement?.textLayerBitmap?.let {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Beyaz Sayfa Çıktısı",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        tonalElevation = 0.dp
                    ) {
                        PreviewImage(it)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            shadowElevation = 1.dp,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(Modifier.padding(14.dp)) {
                Button(
                    onClick = { viewModel.saveCurrentNote(onNotes) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Kaydet", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilledTonalButton(
                        onClick = { copyNote(context, state.note ?: note) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(15.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFFF1F5F9),
                            contentColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Kopyala", style = MaterialTheme.typography.labelLarge)
                    }
                    FilledTonalButton(
                        onClick = { shareNote(context, state.note ?: note) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(15.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFFF1F5F9),
                            contentColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Paylaş", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun NotesScreen(
    state: Board2NotesUiState,
    onOpen: (SavedNote) -> Unit,
    onFolderOpen: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val groups = remember(state.savedNotes) {
        state.savedNotes
            .groupBy { it.courseName.ifBlank { "Genel" } }
            .entries
            .sortedByDescending { (_, notes) -> notes.maxOf { it.updatedAtEpochMs } }
    }
    val searchResults = remember(state.savedNotes, query) {
        val q = query.trim()
        if (q.isBlank()) emptyList()
        else state.savedNotes.filter {
            it.title.contains(q, true) || it.body.contains(q, true) || it.courseName.contains(q, true)
        }
    }
    val recentNotes = remember(state.savedNotes) {
        state.savedNotes.sortedByDescending { it.updatedAtEpochMs }.take(4)
    }
    val latestEpochMs = recentNotes.firstOrNull()?.updatedAtEpochMs
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(16.dp))
        NotesHeroHeader(
            totalCount = state.savedNotes.size,
            latestEpochMs = latestEpochMs
        )
        Spacer(Modifier.height(16.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                androidx.compose.foundation.text.BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 10.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    decorationBox = { inner ->
                        if (query.isEmpty()) {
                            Text(
                                "Not veya ders ara...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        inner()
                    }
                )
                if (query.isNotBlank()) {
                    IconButton(onClick = { query = "" }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Temizle", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        if (query.isNotBlank()) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                if (searchResults.isEmpty()) {
                    EmptyState("Sonuç bulunamadı", "\"$query\" için eşleşen not yok.")
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Arama sonuçları",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                "${searchResults.size} not",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    searchResults.forEach { note ->
                        NoteListCard(note = note, onClick = { onOpen(note) })
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        } else {
            if (state.savedNotes.isEmpty()) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    EmptyState("Henüz not yok", "İlk tahta fotoğrafını işleyerek not oluştur.")
                }
            } else {
                if (recentNotes.isNotEmpty()) {
                    Text(
                        "Son notlar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        recentNotes.forEach { note ->
                            NoteGridCard(
                                note = note,
                                onClick = { onOpen(note) },
                                modifier = Modifier.width(168.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Dersler",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            "${state.savedNotes.size} not",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    groups.chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            row.forEach { (course, notes) ->
                                CourseFolderCard(
                                    courseName = course,
                                    noteCount = notes.size,
                                    thumbnailPath = notes.firstOrNull { it.whitePageImagePath != null }?.whitePageImagePath,
                                    onClick = { onFolderOpen(course) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(14.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun NotesHeroHeader(totalCount: Int, latestEpochMs: Long?) {
    val gradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f)
        )
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Box(
            modifier = Modifier
                .background(gradient)
                .padding(18.dp)
        ) {
            Column {
                Text("Notlarım", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Derslere göre düzenle, hızlı ara.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(999.dp)) {
                        Text(
                            "$totalCount not",
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    if (latestEpochMs != null) {
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Son güncelleme: ${formatDate(latestEpochMs)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteListCard(note: SavedNote, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            FileThumbnail(note.whitePageImagePath)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(note.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (note.courseName.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            note.courseName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    note.preview.ifBlank { "Boş not" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    formatDate(note.updatedAtEpochMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = {
                Text(
                    "Notu silmek istediğinize emin misiniz?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = { Text("Bu işlem geri alınamaz.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDeleted()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Sil")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Vazgeç", color = MaterialTheme.colorScheme.secondary)
                }
            }
        )
    }

    ScreenColumn {
        Spacer(Modifier.height(16.dp))
        if (note == null) {
            StatusBanner("Not yükleniyor...", busy = true)
            return@ScreenColumn
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            shadowElevation = 2.dp,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            note.title.ifBlank { "Başlıksız not" },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            note.courseName.ifBlank { "Kaydedilen B2Note notu" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color(0xFFF1F5F9)
                ) {
                    Text(
                        formatDate(note.updatedAtEpochMs),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        FilePreview(note.whitePageImagePath, "Beyaz sayfa")

        Spacer(Modifier.height(16.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            shadowElevation = 1.dp,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.TextFields,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Not İçeriği",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    note.body.ifBlank { "Boş not" },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 24.sp
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(Modifier.padding(14.dp)) {
                Button(
                    onClick = onEdit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Düzenle", fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    FilledTonalButton(
                        onClick = { copySavedNote(context, note) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(15.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFFF1F5F9),
                            contentColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Kopyala")
                    }
                    FilledTonalButton(
                        onClick = { shareSavedNote(context, note) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(15.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFFF1F5F9),
                            contentColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Paylaş")
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Sil", fontWeight = FontWeight.SemiBold)
                }
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
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun SettingsScreen(state: Board2NotesUiState, viewModel: Board2NotesViewModel, onAbout: () -> Unit) {
    ScreenColumn {
        Spacer(Modifier.height(18.dp))
        SettingsHeroCard()
        Spacer(Modifier.height(16.dp))

        SettingsSection(title = "Uygulama Ayarları") {
            SettingInfoRow(Icons.Default.AutoAwesome, "Tema", "Board2Note mavi/lacivert")
            SettingInfoRow(Icons.Default.Language, "Dil", "Türkçe")
            SettingInfoRow(Icons.Default.Notifications, "Bildirimler", "Kapalı")
        }

        Spacer(Modifier.height(16.dp))

        SettingsSection(title = "Not Ayarları") {
            SettingInfoRow(Icons.Default.Description, "Varsayılan kayıt biçimi", "Düzenlenebilir not")
            SettingInfoRow(Icons.Default.PictureAsPdf, "PDF dışa aktarma", "Paylaşım seçeneklerinden kullanılabilir")
            SettingSwitchRow(Icons.Default.AutoAwesome, "LLM ile düzenleme", "Not oluşturma tercihleri", state.settings.llmEnabled, viewModel::setLlmEnabled)
            Spacer(Modifier.height(12.dp))
            Text(
                "Model 2 önceliği",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                ModeButton("OCR", state.settings.enhancementMode == EnhancementMode.Ocr, Modifier.weight(1f)) {
                    viewModel.setEnhancementMode(EnhancementMode.Ocr)
                }
                ModeButton("Beyaz sayfa", state.settings.enhancementMode == EnhancementMode.VisualNote, Modifier.weight(1f)) {
                    viewModel.setEnhancementMode(EnhancementMode.VisualNote)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Tahta hassasiyeti: ${"%.2f".format(state.settings.threshold)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary
            )
            Slider(state.settings.threshold, viewModel::setThreshold, valueRange = 0.45f..0.60f)
        }

        Spacer(Modifier.height(16.dp))

        SettingsSection(title = "Geliştirici ve OCR") {
            SettingSwitchRow(Icons.Default.BugReport, "Debug modu", "Pipeline çıktıları ve tanılama dosyaları", state.settings.debugMode, viewModel::setDebugMode)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.settings.groqApiKey,
                onValueChange = viewModel::setGroqApiKey,
                label = { Text("Groq API anahtarı") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "OCR motoru",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(Modifier.height(8.dp))
            OcrEngineChoice.entries.forEach { choice ->
                TextButton(onClick = { viewModel.setOcrEngineChoice(choice) }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (state.settings.ocrEngineChoice == choice) "✓ ${choice.name}" else choice.name)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        SettingsSection(title = "Hakkında") {
            SettingActionRow(Icons.Default.Info, "Uygulama bilgisi", "Sürüm ve proje bilgileri", onAbout)
            SettingInfoRow(Icons.Default.Description, "Sürüm bilgisi", "0.1.0 ürün prototipi")
            SettingInfoRow(Icons.Default.Folder, "Proje bilgileri", "Board2Note yapay zekâ destekli not uygulaması")
        }

        Spacer(Modifier.height(16.dp))
        WarningText("TrOCR ve PaddleOCR adapterleri ileride bağlanmak üzere ayrıldı; bu sürümde birincil OCR ML Kit Latin kullanır.")
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun SettingsHeroCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text("Ayarlar", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Uygulama, not ve model tercihlerini yönetin.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun SettingInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsRowIcon(icon)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingSwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsRowIcon(icon)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SettingActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SettingsRowIcon(icon)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(2.dp))
                Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsRowIcon(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(
        modifier = Modifier.size(40.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun AboutScreen() {
    ScreenColumn {
        Spacer(Modifier.height(18.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            shadowElevation = 2.dp,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(Modifier.padding(20.dp)) {
                B2NoteLogo(compact = false)
                Spacer(Modifier.height(18.dp))
                Text(
                    "B2Note hakkında",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Tahta görüntülerini düzenlenebilir ders notlarına dönüştüren yerel-öncelikli Android uygulaması.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(Modifier.padding(16.dp)) {
                AboutInfoLine("Sürüm", "0.1.0 ürün prototipi", Icons.Default.Info)
                AboutInfoLine("Model 1", "ONNX tahta/projeksiyon segmentasyonu", Icons.Default.Tune)
                AboutInfoLine("Model 2", "ONNX iyileştirme, OCR görüntüsü ve beyaz sayfa çıktısı", Icons.Default.AutoAwesome)
                AboutInfoLine("OCR", "ML Kit Text Recognition v2 Latin", Icons.Default.TextFields)
                AboutInfoLine("Gizlilik", "Notlar ve görseller cihazın uygulama alanında saklanır.", Icons.Default.Description, showDivider = false)
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun AboutInfoLine(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    showDivider: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(Modifier.height(3.dp))
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    if (showDivider) {
        Spacer(Modifier.height(14.dp))
        Divider(color = Color(0xFFE2E8F0))
        Spacer(Modifier.height(14.dp))
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
private fun B2NoteLogo(compact: Boolean, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Surface(shape = RoundedCornerShape(if (compact) 10.dp else 16.dp), color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(if (compact) 34.dp else 62.dp)) {
            Canvas(modifier = Modifier.fillMaxSize().padding(7.dp)) {
                drawRoundRect(Color.White, topLeft = Offset(size.width * 0.12f, size.height * 0.08f), size = Size(size.width * 0.72f, size.height * 0.72f))
                drawLine(Color(0xFF0F172A), Offset(size.width * 0.28f, size.height * 0.32f), Offset(size.width * 0.72f, size.height * 0.32f), strokeWidth = 3f)
                drawLine(Color(0xFF0F172A), Offset(size.width * 0.28f, size.height * 0.48f), Offset(size.width * 0.62f, size.height * 0.48f), strokeWidth = 3f)
                drawLine(Color(0xFF60A5FA), Offset(size.width * 0.54f, size.height * 0.72f), Offset(size.width * 0.88f, size.height * 0.96f), strokeWidth = 6f)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                "Board2Note",
                fontWeight = FontWeight.Bold,
                fontSize = if (compact) 20.sp else 34.sp,
                color = MaterialTheme.colorScheme.onBackground
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
            val isActive = index == current
            val isDone = index < current
            val circleColor = when {
                isActive -> MaterialTheme.colorScheme.primary
                isDone -> MaterialTheme.colorScheme.secondary
                else -> Color(0xFFE2E8F0)
            }
            val labelColor = when {
                isActive -> MaterialTheme.colorScheme.primary
                isDone -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Surface(
                    shape = CircleShape,
                    color = circleColor,
                    tonalElevation = if (isActive) 2.dp else 0.dp,
                    modifier = Modifier.size(if (isActive) 32.dp else 28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "${index + 1}",
                            color = if (index <= current) Color.White else Color(0xFF64748B),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
        Text(
            "$label ${value.toInt()}",
            modifier = Modifier.width(64.dp),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Slider(value = value.coerceIn(range.start, range.endInclusive), onValueChange = onChange, valueRange = range)
    }
}

@Composable
private fun FolderScreen(
    state: Board2NotesUiState,
    viewModel: Board2NotesViewModel,
    courseName: String,
    onOpen: (SavedNote) -> Unit
) {
    val notes = remember(state.savedNotes, courseName) {
        state.savedNotes
            .filter { (it.courseName.ifBlank { "Genel" }) == courseName }
            .sortedByDescending { it.updatedAtEpochMs }
    }
    var selectionMode by remember(courseName) { mutableStateOf(false) }
    var selectedIds by remember(courseName) { mutableStateOf(setOf<String>()) }
    var deleteMode by remember { mutableStateOf<FolderDeleteMode?>(null) }

    LaunchedEffect(notes) {
        selectedIds = selectedIds.intersect(notes.map { it.id }.toSet())
        if (selectedIds.isEmpty() && selectionMode && notes.isEmpty()) selectionMode = false
    }

    deleteMode?.let { mode ->
        AlertDialog(
            onDismissRequest = { deleteMode = null },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    if (mode == FolderDeleteMode.Selected) {
                        "Seçili notları silmek istiyor musunuz?"
                    } else {
                        "Bu dersteki tüm notları silmek istiyor musunuz?"
                    },
                    fontWeight = FontWeight.Bold
                )
            },
            text = { Text("Bu işlem geri alınamaz.") },
            confirmButton = {
                Button(
                    onClick = {
                        val ids = if (mode == FolderDeleteMode.Selected) selectedIds else notes.map { it.id }.toSet()
                        viewModel.deleteNotes(ids)
                        selectedIds = emptySet()
                        selectionMode = false
                        deleteMode = null
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(if (mode == FolderDeleteMode.Selected) "Sil" else "Tümünü Sil")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteMode = null }) {
                    Text("Vazgeç", color = MaterialTheme.colorScheme.secondary)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            shadowElevation = 2.dp,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(50.dp),
                        shape = RoundedCornerShape(17.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            courseName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (selectionMode) "${selectedIds.size} not seçildi" else "${notes.size} not",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            selectionMode = !selectionMode
                            if (!selectionMode) selectedIds = emptySet()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(15.dp),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
                        enabled = notes.isNotEmpty()
                    ) {
                        Text(if (selectionMode) "Vazgeç" else "Seç")
                    }
                    OutlinedButton(
                        onClick = { deleteMode = FolderDeleteMode.Selected },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(15.dp),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
                        enabled = selectionMode && selectedIds.isNotEmpty()
                    ) {
                        Text("Seçileni Sil")
                    }
                    Button(
                        onClick = { deleteMode = FolderDeleteMode.All },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(15.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        enabled = notes.isNotEmpty()
                    ) {
                        Text("Tümünü Sil")
                    }
                }
            }
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            if (notes.isEmpty()) {
                FolderEmptyState()
            } else {
                notes.forEach { note ->
                    FolderNoteCard(
                        note = note,
                        selectionMode = selectionMode,
                        selected = selectedIds.contains(note.id),
                        onClick = {
                            if (selectionMode) {
                                selectedIds = if (selectedIds.contains(note.id)) selectedIds - note.id else selectedIds + note.id
                            } else {
                                onOpen(note)
                            }
                        }
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

private enum class FolderDeleteMode { Selected, All }

@Composable
private fun FolderEmptyState() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(14.dp))
            Text("Bu derste henüz not yok.", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                "Yeni bir tahta notu oluşturarak başlayın.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FolderNoteCard(
    note: SavedNote,
    selectionMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionMode) {
                Checkbox(checked = selected, onCheckedChange = { onClick() })
                Spacer(Modifier.width(8.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    note.title.ifBlank { "Başlıksız not" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    formatDate(note.createdAtEpochMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(7.dp))
                Text(
                    note.preview.ifBlank { "Boş not" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(12.dp))
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun CourseFolderCard(
    courseName: String,
    noteCount: Int,
    thumbnailPath: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = folderColor(courseName)
    val bitmap = remember(thumbnailPath) { thumbnailPath?.let { BitmapFactory.decodeFile(it) } }
    val initial = courseName.trim().firstOrNull()?.uppercase() ?: "#"

    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column {
            // Renkli header alanı
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(124.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(color, color.copy(alpha = 0.72f))
                        )
                    ),
                contentAlignment = Alignment.BottomStart
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(color.copy(alpha = 0.55f))
                    )
                }
                Text(
                    text = initial,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.padding(12.dp)
                )
            }
            // Alt bilgi alanı
            Column(modifier = Modifier.padding(12.dp, 10.dp, 12.dp, 12.dp)) {
                Text(
                    courseName,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(5.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = color.copy(alpha = 0.12f)
                ) {
                    Text(
                        "$noteCount not",
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun NoteGridCard(
    note: SavedNote,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(note.whitePageImagePath) {
        note.whitePageImagePath?.let { BitmapFactory.decodeFile(it) }
    }
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            Column(modifier = Modifier.padding(10.dp, 9.dp, 10.dp, 11.dp)) {
                Text(
                    note.title,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    formatDate(note.updatedAtEpochMs).split(" ").take(2).joinToString(" "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun folderColor(courseName: String): Color {
    val palette = listOf(
        Color(0xFF2563EB),
        Color(0xFF1D4ED8),
        Color(0xFF0F172A),
        Color(0xFF334155),
        Color(0xFF475569),
        Color(0xFF1E40AF)
    )
    return palette[Math.abs(courseName.hashCode()) % palette.size]
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
    route == Route.Home -> "Ana Sayfa"
    route == Route.Capture -> "Görsel Seçimi"
    route == Route.Review -> "Görsel Önizleme"
    route == Route.Crop -> "Tahta Alanı Seçimi"
    route == Route.Enhancement -> "Görüntü İyileştirme"
    route == Route.Ocr -> "OCR"
    route == Route.NoteEditor -> "Not Editörü"
    route == Route.Notes -> "Notlarım"
    route?.startsWith(Route.NoteDetail) == true -> "Not Detayı"
    route?.startsWith(Route.Folder) == true -> "Ders Notları"
    route == Route.Settings -> "Ayarlar"
    route == Route.About -> "Hakkında"
    route == Route.Debug -> "Debug"
    else -> "Board2Note"
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
