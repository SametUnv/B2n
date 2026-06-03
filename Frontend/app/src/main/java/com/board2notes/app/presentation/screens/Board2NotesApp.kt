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
import androidx.activity.compose.BackHandler

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

import androidx.compose.foundation.gestures.awaitEachGesture

import androidx.compose.foundation.gestures.detectHorizontalDragGestures

import androidx.compose.foundation.horizontalScroll

import androidx.compose.foundation.interaction.MutableInteractionSource

import androidx.compose.foundation.layout.Arrangement

import androidx.compose.foundation.layout.Box

import androidx.compose.foundation.layout.BoxWithConstraints

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

import androidx.compose.material.icons.filled.Menu

import androidx.compose.material.icons.filled.MoreVert

import androidx.compose.material.icons.filled.Notifications

import androidx.compose.material.icons.filled.PictureAsPdf

import androidx.compose.material.icons.filled.PhotoLibrary

import androidx.compose.material.icons.filled.FlashOn

import androidx.compose.material.icons.filled.Refresh

import androidx.compose.material.icons.filled.Rotate90DegreesCcw

import androidx.compose.material.icons.filled.Save

import androidx.compose.material.icons.filled.Search

import androidx.compose.material.icons.filled.Settings

import androidx.compose.material.icons.filled.Share

import androidx.compose.material.icons.filled.Tune

import androidx.compose.material.icons.filled.Star

import androidx.compose.material.icons.filled.Archive

import androidx.compose.material.icons.filled.AutoAwesome

import androidx.compose.material.icons.filled.TextFields

import androidx.compose.material3.AlertDialog

import androidx.compose.material3.Button

import androidx.compose.material3.ButtonDefaults

import androidx.compose.material3.Checkbox

import androidx.compose.material3.DropdownMenu

import androidx.compose.material3.DropdownMenuItem

import androidx.compose.material3.DrawerValue

import androidx.compose.material3.ExposedDropdownMenuBox

import androidx.compose.material3.ExposedDropdownMenuDefaults

import androidx.compose.material3.FilledTonalButton

import androidx.compose.material3.Divider

import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.compose.material3.Icon

import androidx.compose.material3.IconButton

import androidx.compose.material3.LinearProgressIndicator

import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.ModalDrawerSheet

import androidx.compose.material3.ModalNavigationDrawer

import androidx.compose.material3.NavigationBar

import androidx.compose.material3.NavigationBarItem

import androidx.compose.material3.NavigationBarItemDefaults

import androidx.compose.material3.NavigationDrawerItem

import androidx.compose.material3.NavigationDrawerItemDefaults

import androidx.compose.material3.OutlinedButton

import androidx.compose.material3.OutlinedTextField

import androidx.compose.material3.Scaffold

import androidx.compose.material3.Slider

import androidx.compose.material3.Snackbar

import androidx.compose.material3.SnackbarHost

import androidx.compose.material3.SnackbarHostState

import androidx.compose.material3.Surface

import androidx.compose.material3.SwipeToDismissBox

import androidx.compose.material3.SwipeToDismissBoxValue

import androidx.compose.material3.Switch

import androidx.compose.material3.Tab

import androidx.compose.material3.TabRow

import androidx.compose.material3.Text

import androidx.compose.material3.TextButton

import androidx.compose.material3.CenterAlignedTopAppBar

import androidx.compose.material3.TopAppBar

import androidx.compose.material3.TopAppBarDefaults

import androidx.compose.material3.rememberDrawerState

import androidx.compose.material3.rememberSwipeToDismissBoxState

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

import androidx.compose.ui.geometry.Rect

import androidx.compose.ui.geometry.Size

import androidx.compose.ui.geometry.CornerRadius

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.graphics.PathEffect

import androidx.compose.ui.graphics.asImageBitmap

import androidx.compose.ui.graphics.drawscope.Stroke

import androidx.compose.ui.graphics.drawscope.withTransform

import androidx.compose.ui.graphics.vector.ImageVector

import androidx.compose.ui.input.pointer.pointerInput

import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
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

import com.board2notes.app.data.settings.ADB_REVERSE_BACKEND_BASE_URL

import com.board2notes.app.data.settings.DEFAULT_BACKEND_BASE_URL

import com.board2notes.app.data.settings.EMULATOR_BACKEND_BASE_URL

import com.board2notes.app.data.settings.LAN_BACKEND_BASE_URL

import com.board2notes.app.data.settings.OcrEngineChoice

import com.board2notes.app.domain.model.EnhancementMode

import com.board2notes.app.domain.model.FormattedNote

import com.board2notes.app.domain.model.NoteType

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

import com.board2notes.app.data.image.BitmapLoader

import com.board2notes.app.presentation.canvas.CanvasAssets

import com.board2notes.app.presentation.canvas.CanvasController

import com.board2notes.app.presentation.canvas.CanvasDocument

import com.board2notes.app.presentation.canvas.CanvasDocumentCodec

import com.board2notes.app.presentation.canvas.CanvasEditor
import com.board2notes.app.presentation.text.TextNoteBlock
import com.board2notes.app.presentation.text.TextNoteCodec

import com.board2notes.app.presentation.canvas.CanvasInputSettings

import com.board2notes.app.presentation.canvas.NoteAiAssistant

import com.board2notes.app.presentation.canvas.NoteAiState

import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.withContext

import java.io.File

import java.text.SimpleDateFormat

import java.util.Date

import java.util.Locale

import kotlin.math.abs

import kotlin.math.sqrt

import androidx.compose.material.icons.filled.Close

import androidx.compose.ui.layout.ContentScale

import androidx.compose.ui.layout.onSizeChanged

import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.ui.graphics.Brush

import androidx.compose.animation.core.Animatable

import androidx.compose.animation.core.animateFloatAsState

import androidx.compose.animation.core.rememberInfiniteTransition

import androidx.compose.animation.core.animateFloat

import androidx.compose.animation.core.infiniteRepeatable

import androidx.compose.animation.core.RepeatMode

import androidx.compose.animation.core.LinearEasing

import androidx.compose.animation.AnimatedVisibility

import androidx.compose.animation.expandHorizontally

import androidx.compose.animation.scaleIn

import androidx.compose.animation.scaleOut

import androidx.compose.animation.slideInVertically

import androidx.compose.animation.slideOutVertically

import androidx.compose.animation.shrinkHorizontally

import androidx.compose.ui.platform.LocalDensity

import androidx.compose.ui.res.painterResource

import androidx.compose.ui.unit.IntOffset

import androidx.compose.ui.unit.IntSize

import androidx.compose.ui.graphics.graphicsLayer

import androidx.compose.foundation.border

import androidx.compose.ui.draw.clip

import com.board2notes.app.R

import androidx.compose.foundation.layout.fillMaxHeight

import androidx.compose.foundation.layout.IntrinsicSize

import androidx.compose.foundation.layout.heightIn

import androidx.compose.foundation.layout.widthIn

import androidx.compose.runtime.rememberCoroutineScope

import kotlin.math.roundToInt

import kotlinx.coroutines.delay

import kotlinx.coroutines.launch

import androidx.compose.foundation.layout.offset

import androidx.compose.ui.graphics.Path

import androidx.compose.ui.graphics.StrokeCap

import androidx.compose.ui.graphics.StrokeJoin

import androidx.compose.foundation.gestures.detectDragGestures

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress

import androidx.compose.material.icons.filled.Brush

import androidx.compose.material.icons.filled.Undo

import androidx.compose.material.icons.filled.Add

import androidx.compose.material.icons.filled.Palette

import androidx.compose.ui.graphics.vector.path

import androidx.compose.ui.graphics.SolidColor

import androidx.compose.foundation.combinedClickable

import androidx.compose.ui.layout.boundsInRoot

import androidx.compose.ui.layout.onGloballyPositioned

import androidx.compose.runtime.mutableStateMapOf

import androidx.compose.foundation.ExperimentalFoundationApi

import androidx.compose.foundation.shape.CircleShape

import androidx.compose.ui.graphics.toArgb



private enum class SortBy { Name, Date }



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

    const val Archive = "archive"

    const val Debug = "debug"

    const val Folder = "folder"

    const val Favorites = "favorites"

    const val Trash = "trash"

}



private data class NavigationDestination(

    val route: String,

    val label: String,

    val icon: ImageVector,

    val topLevel: Boolean = true

)



private val mainDrawerDestinations = listOf(

    NavigationDestination(Route.Home, "Ana Sayfa", Icons.Default.Home),

    NavigationDestination(Route.Favorites, "Favoriler", Icons.Default.Star),

    NavigationDestination(Route.Notes, "Tüm Notlar", Icons.Default.Description),

    NavigationDestination(Route.Archive, "Arşivlenenler", Icons.Default.Archive),

    NavigationDestination(Route.Trash, "Çöp Kutusu", Icons.Default.Delete)

)



private val settingsDrawerDestination =

    NavigationDestination(Route.Settings, "Ayarlar", Icons.Default.Settings)



private val customCourseColors = mutableStateMapOf<String, Color>()

private fun persistCustomCourseColors(context: Context) {
    val file = File(context.filesDir, "course_colors.txt")
    runCatching {
        val data = customCourseColors.entries
            .sortedBy { it.key.lowercase() }
            .joinToString("\n") { "${it.key}=${it.value.toArgb()}" }
        file.writeText(data)
    }
}



@OptIn(ExperimentalMaterial3Api::class)

@Composable

fun Board2NotesApp(viewModel: Board2NotesViewModel) {

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val navController = rememberNavController()

    val snackbarHostState = remember { SnackbarHostState() }

    val context = LocalContext.current

    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    var isSearchingHome by remember { mutableStateOf(false) }

    var homeSearchQuery by remember { mutableStateOf("") }

    var showCreateNoteSheet by remember { mutableStateOf(false) }

    var showStartupLoading by remember { mutableStateOf(true) }

    var showTopMenu by remember { mutableStateOf(false) }

    var selectedNoteIds by remember { mutableStateOf(emptySet<String>()) }

    var showBulkDeleteDialog by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val coroutineScope = rememberCoroutineScope()



    var homeSortBy by remember { mutableStateOf(SortBy.Date) }

    var showSortDialog by remember { mutableStateOf(false) }

    var showCreateCourseDialog by remember { mutableStateOf(false) }

    var popupCourseName by remember { mutableStateOf<String?>(null) }



    LaunchedEffect(Unit) {

        delay(650)

        showStartupLoading = false

        // Load custom course colors

        val file = File(context.filesDir, "course_colors.txt")

        if (file.exists()) {

            runCatching {

                file.readLines().forEach { line ->

                    val name = line.substringBefore("=").trim()

                    val value = line.substringAfter("=", "").trim()

                    val argb = value.toIntOrNull()

                    if (name.isNotBlank() && argb != null) {

                        customCourseColors[name] = Color(argb)

                    }

                }

            }

        }

    }



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

    BackHandler(enabled = state.pendingScanNoteId != null && route in setOf(Route.Capture, Route.Review, Route.Crop)) {
        viewModel.cancelPendingScan()
        navController.popBackStack()
    }



    if (showCreateNoteSheet) {

        CreateNoteSheet(

            onDismiss = { showCreateNoteSheet = false },

            onCreateText = {

                showCreateNoteSheet = false

                viewModel.createBlankNote(NoteType.Text) {

                    navController.navigate(Route.NoteEditor)

                }

            },

            onCreateCanvas = {

                showCreateNoteSheet = false

                viewModel.createBlankNote(NoteType.Canvas) {

                    navController.navigate(Route.NoteEditor)

                }

            }

        )

    }



    if (showSortDialog) {

        AlertDialog(

            onDismissRequest = { showSortDialog = false },

            shape = RoundedCornerShape(16.dp),

            containerColor = MaterialTheme.colorScheme.surface,

            title = {

                Text(

                    text = "Notları Sırala",

                    fontWeight = FontWeight.Bold,

                    style = MaterialTheme.typography.titleLarge

                )

            },

            text = {

                Column(modifier = Modifier.fillMaxWidth()) {

                    Surface(

                        onClick = {

                            homeSortBy = SortBy.Name

                            showSortDialog = false

                        },

                        shape = RoundedCornerShape(12.dp),

                        color = if (homeSortBy == SortBy.Name) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else Color.Transparent,

                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)

                    ) {

                        Row(

                            modifier = Modifier.padding(16.dp),

                            verticalAlignment = Alignment.CenterVertically

                        ) {

                            Icon(

                                imageVector = Icons.Default.TextFields,

                                contentDescription = null,

                                tint = if (homeSortBy == SortBy.Name) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

                            )

                            Spacer(Modifier.width(12.dp))

                            Text(

                                text = "Ada Göre",

                                fontWeight = FontWeight.Medium,

                                color = MaterialTheme.colorScheme.onSurface

                            )

                        }

                    }

                    Surface(

                        onClick = {

                            homeSortBy = SortBy.Date

                            showSortDialog = false

                        },

                        shape = RoundedCornerShape(12.dp),

                        color = if (homeSortBy == SortBy.Date) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else Color.Transparent,

                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)

                    ) {

                        Row(

                            modifier = Modifier.padding(16.dp),

                            verticalAlignment = Alignment.CenterVertically

                        ) {

                            Icon(

                                imageVector = Icons.Default.Refresh,

                                contentDescription = null,

                                tint = if (homeSortBy == SortBy.Date) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

                            )

                            Spacer(Modifier.width(12.dp))

                            Text(

                                text = "Tarihe Göre",

                                fontWeight = FontWeight.Medium,

                                color = MaterialTheme.colorScheme.onSurface

                            )

                        }

                    }

                }

            },

            confirmButton = {

                TextButton(onClick = { showSortDialog = false }) {

                    Text("Kapat")

                }

            }

        )

    }



    if (showCreateCourseDialog) {

        var courseNameInput by remember { mutableStateOf("") }

        val colorsList = listOf(

            Color(0xFF2563EB),

            Color(0xFF0D9488),

            Color(0xFF1B5E20),

            Color(0xFFF59E0B),

            Color(0xFF7C3AED),

            Color(0xFFDC2626)

        )

        var selectedColorIndex by remember { mutableStateOf(0) }



        AlertDialog(

            onDismissRequest = { showCreateCourseDialog = false },

            shape = RoundedCornerShape(20.dp),

            containerColor = MaterialTheme.colorScheme.surface,

            title = {

                Text(

                    text = "Yeni Ders Oluştur",

                    fontWeight = FontWeight.Bold,

                    style = MaterialTheme.typography.titleLarge

                )

            },

            text = {

                Column(modifier = Modifier.fillMaxWidth()) {

                    OutlinedTextField(

                        value = courseNameInput,

                        onValueChange = { courseNameInput = it },

                        label = { Text("Ders Adı") },

                        singleLine = true,

                        modifier = Modifier.fillMaxWidth(),

                        shape = RoundedCornerShape(12.dp)

                    )

                    Spacer(Modifier.height(16.dp))

                    Text(

                        text = "Ders Rengi Seçin",

                        style = MaterialTheme.typography.bodyMedium,

                        fontWeight = FontWeight.Medium,

                        color = MaterialTheme.colorScheme.onSurfaceVariant

                    )

                    Spacer(Modifier.height(8.dp))

                    Row(

                        modifier = Modifier.fillMaxWidth(),

                        horizontalArrangement = Arrangement.SpaceBetween

                    ) {

                        colorsList.forEachIndexed { idx, color ->

                            Box(

                                modifier = Modifier

                                    .size(36.dp)

                                    .background(color, shape = CircleShape)

                                    .border(

                                        width = if (selectedColorIndex == idx) 3.dp else 0.dp,

                                        color = if (selectedColorIndex == idx) MaterialTheme.colorScheme.onSurface else Color.Transparent,

                                        shape = CircleShape

                                    )

                                    .clickable { selectedColorIndex = idx },

                                contentAlignment = Alignment.Center

                            ) {

                                if (selectedColorIndex == idx) {

                                    Icon(

                                        imageVector = Icons.Default.Check,

                                        contentDescription = null,

                                        tint = Color.White,

                                        modifier = Modifier.size(16.dp)

                                    )

                                }

                            }

                        }

                    }

                }

            },

            confirmButton = {

                Button(

                    onClick = {

                        val trimmed = courseNameInput.trim()

                        if (trimmed.isNotBlank()) {

                            customCourseColors[trimmed] = colorsList[selectedColorIndex]

                            persistCustomCourseColors(context)

                            viewModel.showMessage("$trimmed dersi olusturuldu.")



                        }

                        showCreateCourseDialog = false

                    },

                    shape = RoundedCornerShape(12.dp)

                ) {

                    Text("Oluştur")

                }

            },

            dismissButton = {

                TextButton(onClick = { showCreateCourseDialog = false }) {

                    Text("Vazgeç")

                }

            }

        )

    }



    ModalNavigationDrawer(

        drawerState = drawerState,

        drawerContent = {

            Board2NoteNavigationDrawer(

                currentRoute = route,

                state = state,

                onDestinationClick = { destination ->

                    coroutineScope.launch { drawerState.close() }

                    if (destination.topLevel) {

                        navigateTopLevel(destination.route, navController, viewModel)

                    } else {

                        navController.navigate(destination.route) {

                            launchSingleTop = true

                        }

                    }

                }

            )

        }

    ) {

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

            if (route != Route.NoteEditor) {

                if (route == Route.Home && selectedNoteIds.isNotEmpty()) {

                    CenterAlignedTopAppBar(

                        title = {

                            Text(

                                "${selectedNoteIds.size} Seçildi",

                                style = MaterialTheme.typography.titleMedium,

                                fontWeight = FontWeight.Bold,

                                color = Color.White

                            )

                        },

                        navigationIcon = {

                            IconButton(onClick = { selectedNoteIds = emptySet() }) {

                                Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Color.White)

                            }

                        },

                        actions = {

                            IconButton(onClick = {

                                selectedNoteIds.forEach { viewModel.archiveNote(it) }

                                selectedNoteIds = emptySet()

                            }) {

                                Icon(Icons.Default.Folder, contentDescription = "Arşivle", tint = Color.White)

                            }

                            IconButton(onClick = {

                                showBulkDeleteDialog = true

                            }) {

                                Icon(Icons.Default.Delete, contentDescription = "Sil", tint = Color.White)

                            }

                        },

                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(

                            containerColor = Color(0xFF0F172A),

                            titleContentColor = Color.White,

                            navigationIconContentColor = Color.White,

                            actionIconContentColor = Color.White

                        )

                    )

                } else {

                    CenterAlignedTopAppBar(

                        title = {

                            if (route == Route.Home && isSearchingHome) {

                                androidx.compose.foundation.text.BasicTextField(

                                    value = homeSearchQuery,

                                    onValueChange = { homeSearchQuery = it },

                                    textStyle = MaterialTheme.typography.bodyLarge.copy(

                                        color = MaterialTheme.colorScheme.onBackground

                                    ),

                                    singleLine = true,

                                    modifier = Modifier

                                        .fillMaxWidth()

                                        .padding(end = 16.dp)

                                        .background(

                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),

                                            RoundedCornerShape(12.dp)

                                        )

                                        .padding(horizontal = 12.dp, vertical = 8.dp),

                                    decorationBox = { innerTextField ->

                                        if (homeSearchQuery.isEmpty()) {

                                            Text(

                                                "Notlarda arayın...",

                                                style = MaterialTheme.typography.bodyLarge.copy(

                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)

                                                )

                                            )

                                        }

                                        innerTextField()

                                    }

                                )

                            } else if (topLevel) {

                                HeaderLogoMark()

                            } else {

                                Text(

                                    titleForRoute(route),

                                    style = MaterialTheme.typography.titleLarge,

                                    fontWeight = FontWeight.Bold,

                                    color = MaterialTheme.colorScheme.onBackground,

                                    maxLines = 1,

                                    overflow = TextOverflow.Ellipsis

                                )

                            }

                        },

                        navigationIcon = {

                            if (!topLevel) {

                                IconButton(onClick = {
                                    if (route in setOf(Route.Capture, Route.Review)) viewModel.cancelPendingScan()
                                    navController.popBackStack()
                                }) {

                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")

                                }

                            } else {

                                IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {

                                    Icon(Icons.Default.Menu, contentDescription = "Menü")

                                }

                            }

                        },

                        actions = {

                            if (route == Route.Home) {

                                IconButton(onClick = {

                                    isSearchingHome = !isSearchingHome

                                    if (!isSearchingHome) homeSearchQuery = ""

                                }) {

                                    Icon(

                                        imageVector = if (isSearchingHome) Icons.Default.Close else Icons.Default.Search,

                                        contentDescription = "Arama"

                                    )

                                }

}

                            if (topLevel) {

                                Box {

                                    IconButton(onClick = { showTopMenu = true }) {

                                        Icon(Icons.Default.MoreVert, contentDescription = "Daha fazla")

                                    }

                                    TopLevelOverflowMenu(

                                        expanded = showTopMenu,

                                        onDismiss = { showTopMenu = false },

                                        onHome = {

                                            showTopMenu = false

                                            navigateTopLevel(Route.Home, navController, viewModel)

                                        },

                                        onNotes = {

                                            showTopMenu = false

                                            navigateTopLevel(Route.Notes, navController, viewModel)

                                        },

                                        onArchive = {

                                            showTopMenu = false

                                            navController.navigate(Route.Archive)

                                        },

                                        onSettings = {

                                            showTopMenu = false

                                            navigateTopLevel(Route.Settings, navController, viewModel)

                                        },

                                        showHomeOptions = route == Route.Home,

                                        onStartMultiSelect = {

                                            showTopMenu = false

                                            val firstNote = state.savedNotes.firstOrNull { !it.isArchived && !it.isDeleted }

                                            if (firstNote != null) {

                                                selectedNoteIds = setOf(firstNote.id)

                                            }

                                        },

                                        onSort = {

                                            showTopMenu = false

                                            showSortDialog = true

                                        },

                                        onCreateCourse = {

                                            showTopMenu = false

                                            showCreateCourseDialog = true

                                        }

                                    )

                                }

                            }

                            if (state.settings.debugMode && state.note != null) {

                                IconButton(onClick = { navController.navigate(Route.Debug) }) {

                                    Icon(Icons.Default.BugReport, contentDescription = "Debug")

                                }

                            }

                        },

                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(

                            containerColor = MaterialTheme.colorScheme.background,

                            titleContentColor = MaterialTheme.colorScheme.onBackground,

                            navigationIconContentColor = MaterialTheme.colorScheme.onBackground,

                            actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant

                        )

                    )

                }

            }

        },

        bottomBar = {

            if (topLevel && state.settings.showBottomNavigation) {

                B2NoteBottomBar(route = route, navController = navController, viewModel = viewModel)

            }

        },

        floatingActionButton = {

            if (route == Route.Home || route == Route.Notes) {

                Surface(

                    onClick = { showCreateNoteSheet = true },

                    shape = RoundedCornerShape(20.dp),

                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),

                    contentColor = MaterialTheme.colorScheme.primary,

                    modifier = Modifier

                        .padding(bottom = 0.dp)

                        .shadow(

                            elevation = 8.dp,

                            shape = RoundedCornerShape(20.dp),

                            ambientColor = Color(0x1F000000),

                            spotColor = Color(0x2F000000)

                        ),

                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f))

                ) {

                    Box(

                        modifier = Modifier.size(56.dp),

                        contentAlignment = Alignment.Center

                    ) {

                        Icon(

                            imageVector = Icons.Default.Add,

                            contentDescription = "Yeni Not Oluştur",

                            modifier = Modifier.size(26.dp)

                        )

                    }

                }

            }

        }

    ) { padding ->

        Box(

            modifier = Modifier

                .fillMaxSize()

                .padding(

                    top = padding.calculateTopPadding(),

                    bottom = if (topLevel) 0.dp else padding.calculateBottomPadding()

                )

        ) {

            if (state.isBusy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

            NavHost(

                navController = navController,

                startDestination = Route.Home,

                enterTransition = { fadeIn(tween(160)) },

                exitTransition = { fadeOut(tween(120)) },

                popEnterTransition = { fadeIn(tween(160)) },

                popExitTransition = { fadeOut(tween(120)) }

            ) {

                composable(Route.Home) {

                    HomeScreen(

                        state = state,

                        searchQuery = homeSearchQuery,

                        selectedNoteIds = selectedNoteIds,

                        onSelectionChange = { selectedNoteIds = it },

                        onStart = { showCreateNoteSheet = true },

                        onOpenNote = { note ->

                            viewModel.openSavedNote(note.id)

                            navController.navigate(Route.NoteEditor)

                        },

                        onFolderOpen = { course ->

                            popupCourseName = course

                        },

                        onArchiveNote = viewModel::archiveNote,

                        onDeleteNote = viewModel::deleteNoteById,

                        sortBy = homeSortBy,

                        viewModel = viewModel

                    )

                }

                composable(Route.Capture) {

                    SamsungCaptureScreen(

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

                    SamsungImageReviewScreen(

                        state = state,

                        viewModel = viewModel,

                        onContinue = {

                            if (state.pendingScanNoteId != null) {

                                viewModel.detectBackendBoardForPendingNote {

                                    navController.navigate(Route.Crop) {

                                        popUpTo(Route.Review) { inclusive = true }

                                    }

                                }

                            } else {

                                viewModel.detectBoard()

                                navController.navigate(Route.Crop)

                            }

                        }

                    )

                }

                composable(Route.Crop) {

                    BoardCropScreen(

                        state = state,

                        viewModel = viewModel,

                        onContinue = {

                            if (state.pendingScanNoteId != null) {

                                viewModel.runBackendPipelineForPendingNote {

                                    navController.navigate(Route.NoteEditor) {

                                        popUpTo(Route.Capture) { inclusive = true }

                                    }

                                }

                            } else {

                                viewModel.enhanceBoard()

                                navController.navigate(Route.Enhancement)

                            }

                        },

                        onSkipToNote = {

                            if (state.pendingScanNoteId != null) {

                                viewModel.cancelPendingScan()

                            } else {

                                viewModel.skipToNoteEditor()

                            }

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

                    SamsungStyleNoteEditorScreen(

                        state = state,

                        viewModel = viewModel,

                        onScan = { pageIndex ->

                            viewModel.startScanForCurrentNote(pageIndex)

                            navController.navigate(Route.Capture)

                        },

                        onNotes = {

                            viewModel.refreshNotes()

                            val prevRoute = navController.previousBackStackEntry?.destination?.route

                            if (prevRoute == Route.Home ||
                                prevRoute == Route.Notes ||
                                prevRoute == Route.Archive ||
                                prevRoute == Route.Favorites ||
                                prevRoute == Route.Trash ||
                                (prevRoute != null && prevRoute.startsWith(Route.Folder)) ||
                                (prevRoute != null && prevRoute.startsWith(Route.NoteDetail))
                            ) {

                                navController.popBackStack()

                            } else {

                                navigateTopLevel(Route.Notes, navController, viewModel)

                            }

                        }

                    )

                }

                composable(Route.Notes) {

                    LaunchedEffect(Unit) { viewModel.refreshNotes() }

                    NotesScreen(

                        state = state,

                        onOpen = { note ->

                            viewModel.openSavedNote(note.id)

                            navController.navigate(Route.NoteEditor)

                        },

                        onFolderOpen = { course ->

                            popupCourseName = course

                        },

                        onArchiveOpen = { navController.navigate(Route.Archive) }

                    )

                }

                composable(Route.Archive) {

                    LaunchedEffect(Unit) { viewModel.refreshNotes() }

                    ArchiveScreen(

                        state = state,

                        viewModel = viewModel,

                        onOpen = { note ->

                            viewModel.openSavedNote(note.id)

                            navController.navigate(Route.NoteEditor)

                        }

                    )

                }

                composable(Route.Favorites) {

                    LaunchedEffect(Unit) { viewModel.refreshNotes() }

                    FavoritesScreen(

                        state = state,

                        viewModel = viewModel,

                        onOpen = { note ->

                            viewModel.openSavedNote(note.id)

                            navController.navigate(Route.NoteEditor)

                        }

                    )

                }

                composable(Route.Trash) {

                    LaunchedEffect(Unit) { viewModel.refreshNotes() }

                    TrashScreen(

                        state = state,

                        viewModel = viewModel,

                        onOpen = { note ->

                            viewModel.openSavedNote(note.id)

                            navController.navigate(Route.NoteEditor)

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

                    SamsungSettingsScreen(

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

            popupCourseName?.let { course ->

                CourseFolderPopup(
                    courseName = course,
                    notes = state.savedNotes.filter {
                        !it.isDeleted &&
                            !it.isArchived &&
                            it.courseName.ifBlank { "Genel" } == course
                    },
                    onDismiss = { popupCourseName = null },
                    onOpenNote = { note ->
                        popupCourseName = null
                        viewModel.openSavedNote(note.id)
                        navController.navigate(Route.NoteEditor)
                    },
                    onCreateNote = {
                        popupCourseName = null
                        showCreateNoteSheet = true
                    },
                    onDeleteCourse = {
                        customCourseColors.remove(course)
                        persistCustomCourseColors(context)
                        viewModel.moveCourseNotesToGeneral(course)
                        popupCourseName = null
                    }
                )

            }

            if (state.isBusy) {

                BoardLoadingOverlay(message = loadingMessage(state))

            } else if (showStartupLoading) {

                BoardLoadingOverlay(message = "Not alanı hazırlanıyor.")

            }

        }

    }

}

}



@Composable

private fun Board2NoteNavigationDrawer(

    currentRoute: String?,

    state: Board2NotesUiState,

    onDestinationClick: (NavigationDestination) -> Unit

) {
    val appbarLogo = board2NoteAppbarLogoRes()

    ModalDrawerSheet(

        drawerContainerColor = MaterialTheme.colorScheme.surface,

        drawerContentColor = MaterialTheme.colorScheme.onSurface,

        modifier = Modifier.width(304.dp)

    ) {

        Column(

            modifier = Modifier

                .fillMaxHeight()

                .background(

                    Brush.verticalGradient(

                        colors = listOf(

                            MaterialTheme.colorScheme.surface,

                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

                        )

                    )

                )

                .padding(horizontal = 16.dp, vertical = 18.dp)

        ) {

            // Centered Premium B2N Logo

            Box(

                modifier = Modifier

                    .fillMaxWidth()

                    .padding(vertical = 24.dp),

                contentAlignment = Alignment.Center

            ) {

                Image(

                    painter = painterResource(id = appbarLogo),

                    contentDescription = "B2N Logo",

                    modifier = Modifier

                        .width(160.dp)

                        .height(72.dp)

                )

            }

            

            Spacer(Modifier.height(16.dp))



            mainDrawerDestinations.forEach { destination ->

                val selected = when (destination.route) {

                    Route.Notes -> currentRoute == Route.Notes || currentRoute?.startsWith("${Route.Folder}/") == true

                    Route.Archive -> currentRoute == Route.Archive

                    else -> currentRoute == destination.route

                }

                

                Row(

                    verticalAlignment = Alignment.CenterVertically,

                    modifier = Modifier

                        .fillMaxWidth()

                        .padding(vertical = 3.dp)

                ) {

                    // Modern left pill capsule indicator for active selection

                    if (selected) {

                        Surface(

                            modifier = Modifier

                                .width(4.dp)

                                .height(28.dp)

                                .clip(RoundedCornerShape(2.dp)),

                            color = MaterialTheme.colorScheme.primary

                        ) {}

                    } else {

                        Spacer(Modifier.width(4.dp))

                    }

                    Spacer(Modifier.width(8.dp))



                    NavigationDrawerItem(

                        label = {

                            Row(verticalAlignment = Alignment.CenterVertically) {

                                Text(

                                    text = destination.label,

                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,

                                    style = MaterialTheme.typography.bodyMedium

                                )

                                val count = when (destination.route) {

                                    Route.Favorites -> state.favoriteNotes.size

                                    Route.Notes -> state.savedNotes.size

                                    Route.Archive -> state.archivedNotes.size

                                    Route.Trash -> state.deletedNotes.size

                                    else -> 0

                                }

                                if (count > 0) {

                                    Spacer(Modifier.width(8.dp))

                                    Surface(

                                        shape = CircleShape,

                                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer

                                    ) {

                                        Text(

                                            "$count",

                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),

                                            style = MaterialTheme.typography.labelSmall,

                                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,

                                            fontWeight = FontWeight.SemiBold

                                        )

                                    }

                                }

                            }

                        },

                        selected = selected,

                        onClick = { onDestinationClick(destination) },

                        icon = { Icon(destination.icon, contentDescription = null, modifier = Modifier.size(20.dp)) },

                        colors = NavigationDrawerItemDefaults.colors(

                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,

                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,

                            selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,

                            unselectedContainerColor = Color.Transparent,

                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),

                            unselectedTextColor = MaterialTheme.colorScheme.onSurface

                        ),

                        modifier = Modifier.weight(1f)

                    )

                }

            }



            if (state.settings.debugMode) {

                val selected = currentRoute == Route.Debug

                Row(

                    verticalAlignment = Alignment.CenterVertically,

                    modifier = Modifier

                        .fillMaxWidth()

                        .padding(vertical = 3.dp)

                ) {

                    if (selected) {

                        Surface(

                            modifier = Modifier

                                .width(4.dp)

                                .height(28.dp)

                                .clip(RoundedCornerShape(2.dp)),

                            color = MaterialTheme.colorScheme.primary

                        ) {}

                    } else {

                        Spacer(Modifier.width(4.dp))

                    }

                    Spacer(Modifier.width(8.dp))



                    NavigationDrawerItem(

                        label = { Text("Debug", style = MaterialTheme.typography.bodyMedium) },

                        selected = selected,

                        onClick = { onDestinationClick(NavigationDestination(Route.Debug, "Debug", Icons.Default.BugReport, topLevel = false)) },

                        icon = { Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(20.dp)) },

                        colors = NavigationDrawerItemDefaults.colors(

                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,

                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,

                            selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,

                            unselectedContainerColor = Color.Transparent,

                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),

                            unselectedTextColor = MaterialTheme.colorScheme.onSurface

                        ),

                        modifier = Modifier.weight(1f)

                    )

                }

            }

            Spacer(Modifier.weight(1f))

            Divider(color = MaterialTheme.colorScheme.outlineVariant)

            Spacer(Modifier.height(8.dp))

            

            val settingsSelected = currentRoute == Route.Settings

            Row(

                verticalAlignment = Alignment.CenterVertically,

                modifier = Modifier.fillMaxWidth()

            ) {

                if (settingsSelected) {

                    Surface(

                        modifier = Modifier

                            .width(4.dp)

                            .height(28.dp)

                            .clip(RoundedCornerShape(2.dp)),

                        color = MaterialTheme.colorScheme.primary

                    ) {}

                } else {

                    Spacer(Modifier.width(4.dp))

                }

                Spacer(Modifier.width(8.dp))



                NavigationDrawerItem(

                    label = { Text(settingsDrawerDestination.label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium) },

                    selected = settingsSelected,

                    onClick = { onDestinationClick(settingsDrawerDestination) },

                    icon = { Icon(settingsDrawerDestination.icon, contentDescription = null, modifier = Modifier.size(20.dp)) },

                    colors = NavigationDrawerItemDefaults.colors(

                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,

                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,

                        selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,

                        unselectedContainerColor = Color.Transparent,

                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),

                        unselectedTextColor = MaterialTheme.colorScheme.onSurface

                    ),

                    modifier = Modifier.weight(1f)

                )

            }

        }

    }

}



@Composable

private fun TopLevelOverflowMenu(

    expanded: Boolean,

    onDismiss: () -> Unit,

    onHome: () -> Unit,

    onNotes: () -> Unit,

    onArchive: () -> Unit,

    onSettings: () -> Unit,

    showHomeOptions: Boolean = false,

    onStartMultiSelect: (() -> Unit)? = null,

    onSort: (() -> Unit)? = null,

    onCreateCourse: (() -> Unit)? = null

) {

    DropdownMenu(

        expanded = expanded,

        onDismissRequest = onDismiss,

        modifier = Modifier

            .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp))

            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))

            .width(220.dp),

        shape = RoundedCornerShape(16.dp)

    ) {

        val itemColor = MaterialTheme.colorScheme.onSurface



        DropdownMenuItem(

            text = { Text("Ana sayfa", fontWeight = FontWeight.Medium, color = itemColor) },

            leadingIcon = { Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary) },

            onClick = onHome

        )

        DropdownMenuItem(

            text = { Text("Notlar", fontWeight = FontWeight.Medium, color = itemColor) },

            leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary) },

            onClick = onNotes

        )

        DropdownMenuItem(

            text = { Text("Arşivlenenler", fontWeight = FontWeight.Medium, color = itemColor) },

            leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color(0xFF1B5E20)) },

            onClick = onArchive

        )

        DropdownMenuItem(

            text = { Text("Ayarlar", fontWeight = FontWeight.Medium, color = itemColor) },

            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.secondary) },

            onClick = onSettings

        )



        if (showHomeOptions) {

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            DropdownMenuItem(

                text = { Text("Çoklu Seç", fontWeight = FontWeight.Medium, color = itemColor) },

                leadingIcon = { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary) },

                onClick = { onStartMultiSelect?.invoke() }

            )

            DropdownMenuItem(

                text = { Text("Sırala", fontWeight = FontWeight.Medium, color = itemColor) },

                leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.secondary) },

                onClick = { onSort?.invoke() }

            )

            DropdownMenuItem(

                text = { Text("Ders Oluştur", fontWeight = FontWeight.Medium, color = itemColor) },

                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color(0xFF1B5E20)) },

                onClick = { onCreateCourse?.invoke() }

            )

        }

    }

}



@Composable

private fun CreateNoteSheet(

    onDismiss: () -> Unit,

    onCreateText: () -> Unit,

    onCreateCanvas: () -> Unit

) {

    AlertDialog(

        onDismissRequest = onDismiss,

        shape = RoundedCornerShape(24.dp),

        containerColor = MaterialTheme.colorScheme.surface,

        title = {

            Text(

                "Yeni not",

                style = MaterialTheme.typography.titleLarge,

                fontWeight = FontWeight.Bold

            )

        },

        text = {

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                NoteTypeChoiceRow(

                    title = "Yazı Notu",

                    subtitle = "OCR metni ve elle yazılan ders notları",

                    icon = Icons.Default.TextFields,

                    onClick = onCreateText

                )

                NoteTypeChoiceRow(

                    title = "Canvas Notu",

                    subtitle = "Tahta çıktıları, çizim ve serbest sayfa",

                    icon = Icons.Default.Brush,

                    onClick = onCreateCanvas

                )

            }

        },

        confirmButton = {},

        dismissButton = {

            TextButton(onClick = onDismiss) {

                Text("Vazgeç")

            }

        }

    )

}



@Composable
private fun darkAwareColor(lightColor: Color, darkColor: Color): Color {
    return if (MaterialTheme.colorScheme.background.red < 0.2f) darkColor else lightColor
}

@Composable
private fun board2NoteAppbarLogoRes(): Int {
    return board2NoteAppbarLogoRes(MaterialTheme.colorScheme.background.red < 0.2f)
}

private fun board2NoteAppbarLogoRes(isDark: Boolean): Int {
    return if (isDark) R.drawable.b2n_appbar_beyaz else R.drawable.b2n_appbar
}

@Composable
private fun NoteTypeChoiceRow(

    title: String,

    subtitle: String,

    icon: androidx.compose.ui.graphics.vector.ImageVector,

    onClick: () -> Unit

) {

    Surface(

        modifier = Modifier

            .fillMaxWidth()

            .clickable(onClick = onClick),

        shape = RoundedCornerShape(16.dp),

        color = darkAwareColor(Color(0xFFF8FAFC), MaterialTheme.colorScheme.surfaceVariant),

        border = BorderStroke(1.dp, darkAwareColor(Color(0xFFE2E8F0), MaterialTheme.colorScheme.outlineVariant))

    ) {

        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {

            Surface(

                modifier = Modifier.size(40.dp),

                shape = RoundedCornerShape(13.dp),

                color = MaterialTheme.colorScheme.primaryContainer

            ) {

                Box(contentAlignment = Alignment.Center) {

                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))

                }

            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {

                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

                Spacer(Modifier.height(2.dp))

                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            }

            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)

        }

    }

}



@Composable

private fun BoardLoadingOverlay(message: String) {

    val transition = rememberInfiniteTransition(label = "board-loading")

    val animProgress by transition.animateFloat(

        initialValue = 0f,

        targetValue = 1f,

        animationSpec = infiniteRepeatable(

            animation = tween(durationMillis = 1500, easing = LinearEasing),

            repeatMode = RepeatMode.Restart

        ),

        label = "compact-loading-progress"

    )



    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val appbarLogo = board2NoteAppbarLogoRes(isDark)
    val loadingTrackColor = if (isDark) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f) else Color(0xFFE5E7EB)
    val loadingProgressColor = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF2563EB)

    Box(

        modifier = Modifier

            .fillMaxSize()

            .background(if (isDark) Color(0xFF050914).copy(alpha = 0.72f) else Color(0xFFF8FAFC).copy(alpha = 0.72f)),

        contentAlignment = Alignment.Center

    ) {

        Surface(

            modifier = Modifier

                .widthIn(max = 236.dp)

                .fillMaxWidth()

                .padding(horizontal = 24.dp),

            shape = RoundedCornerShape(18.dp),

            color = if (isDark) MaterialTheme.colorScheme.surface else Color.White,

            tonalElevation = 4.dp,

            shadowElevation = 12.dp,

            border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFFE2E8F0))

        ) {

            Column(

                modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp),

                horizontalAlignment = Alignment.CenterHorizontally

            ) {

                Image(

                    painter = painterResource(id = appbarLogo),

                    contentDescription = "Board2Note",

                    modifier = Modifier

                        .width(132.dp)

                        .height(46.dp),

                    contentScale = androidx.compose.ui.layout.ContentScale.Fit

                )

                Spacer(Modifier.height(12.dp))

                Canvas(

                    modifier = Modifier

                        .width(148.dp)

                        .height(5.dp)

                ) {

                    val radius = CornerRadius(size.height / 2f, size.height / 2f)

                    drawRoundRect(

                        color = loadingTrackColor,

                        size = size,

                        cornerRadius = radius

                    )

                    val segmentWidth = size.width * 0.34f

                    val x = ((size.width + segmentWidth) * animProgress) - segmentWidth

                    val visibleStart = x.coerceIn(0f, size.width)

                    val visibleWidth = (segmentWidth - (visibleStart - x)).coerceIn(0f, size.width - visibleStart)

                    drawRoundRect(

                        color = loadingProgressColor,

                        topLeft = Offset(visibleStart, 0f),

                        size = Size(visibleWidth, size.height),

                        cornerRadius = radius

                    )

                }

                Spacer(Modifier.height(12.dp))

                Text(

                    message,

                    style = MaterialTheme.typography.labelSmall,

                    color = MaterialTheme.colorScheme.onSurfaceVariant,

                    textAlign = TextAlign.Center,

                    maxLines = 2,

                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis

                )

            }

        }

    }

}



private fun loadingMessage(state: Board2NotesUiState): String = when {

    !state.loadingMessage.isNullOrBlank() -> state.loadingMessage

    state.pendingScanNoteId != null -> "Tahta görüntüsü işleniyor."

    state.screen == com.board2notes.app.presentation.state.AppScreen.ImageReview -> "Görsel hazırlanıyor."

    state.screen == com.board2notes.app.presentation.state.AppScreen.BoardDetection -> "Tahta alanı bulunuyor."

    state.screen == com.board2notes.app.presentation.state.AppScreen.Enhancement -> "Model 2 görüntüyü iyileştiriyor."

    state.screen == com.board2notes.app.presentation.state.AppScreen.Ocr -> "OCR metni çıkarılıyor."

    else -> "İşlem devam ediyor."

}



@Composable

private fun B2NoteBottomBar(route: String?, navController: NavHostController, viewModel: Board2NotesViewModel) {

    Box(

        modifier = Modifier

            .fillMaxWidth()

            .padding(horizontal = 24.dp, vertical = 16.dp)

    ) {

        Surface(

            modifier = Modifier

                .fillMaxWidth()

                .shadow(

                    elevation = 12.dp,

                    shape = RoundedCornerShape(28.dp),

                    ambientColor = Color(0x1F000000),

                    spotColor = Color(0x2F000000)

                ),

            shape = RoundedCornerShape(28.dp),

            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),

            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),

            tonalElevation = 0.dp

        ) {

            Row(

                modifier = Modifier

                    .fillMaxWidth()

                    .padding(horizontal = 12.dp, vertical = 8.dp),

                horizontalArrangement = Arrangement.SpaceAround,

                verticalAlignment = Alignment.CenterVertically

            ) {

                B2NoteNavItem(

                    label = "Ana Sayfa",

                    icon = Icons.Default.Home,

                    selected = route == Route.Home,

                    onClick = { navigateTopLevel(Route.Home, navController, viewModel) }

                )

                B2NoteNavItem(

                    label = "Notlarım",

                    icon = Icons.Default.Folder,

                    selected = route == Route.Notes,

                    onClick = { navigateTopLevel(Route.Notes, navController, viewModel) }

                )

                B2NoteNavItem(

                    label = "Ayarlar",

                    icon = Icons.Default.Settings,

                    selected = route == Route.Settings,

                    onClick = { navigateTopLevel(Route.Settings, navController, viewModel) }

                )

            }

        }

    }

}



@Composable

private fun B2NoteNavItem(

    label: String,

    icon: androidx.compose.ui.graphics.vector.ImageVector,

    selected: Boolean,

    modifier: Modifier = Modifier,

    onClick: () -> Unit

) {

    val scale = animateFloatAsState(

        targetValue = if (selected) 1.05f else 1.0f,

        animationSpec = tween(durationMillis = 300),

        label = "NavItemScale"

    )



    Box(

        modifier = modifier

            .graphicsLayer(scaleX = scale.value, scaleY = scale.value)

            .clip(RoundedCornerShape(20.dp))

            .clickable(onClick = onClick)

            .then(

                if (selected) {

                    Modifier

                        .background(

                            Brush.verticalGradient(

                                listOf(

                                    darkAwareColor(Color(0xFFEFF6FF), MaterialTheme.colorScheme.primaryContainer).copy(alpha = 0.9f),

                                    darkAwareColor(Color(0xFFDBEAFE), MaterialTheme.colorScheme.surfaceVariant).copy(alpha = 0.7f)

                                )

                            )

                        )

                        .border(

                            BorderStroke(1.dp, darkAwareColor(Color(0xFF93C5FD), MaterialTheme.colorScheme.primary).copy(alpha = 0.5f)),

                            RoundedCornerShape(20.dp)

                        )

                } else {

                    Modifier

                }

            )

            .padding(horizontal = 16.dp, vertical = 10.dp),

        contentAlignment = Alignment.Center

    ) {

        Row(

            horizontalArrangement = Arrangement.Center,

            verticalAlignment = Alignment.CenterVertically

        ) {

            Icon(

                icon,

                contentDescription = null,

                tint = if (selected) darkAwareColor(Color(0xFF1D4ED8), MaterialTheme.colorScheme.primary) else MaterialTheme.colorScheme.onSurfaceVariant,

                modifier = Modifier.size(22.dp)

            )

            AnimatedVisibility(

                visible = selected,

                enter = fadeIn(tween(150)) + expandHorizontally(tween(150)),

                exit = fadeOut(tween(150)) + shrinkHorizontally(tween(150))

            ) {

                Row {

                    Spacer(Modifier.width(8.dp))

                    Text(

                        label,

                        maxLines = 1,

                        overflow = TextOverflow.Ellipsis,

                        style = MaterialTheme.typography.labelMedium,

                        fontWeight = FontWeight.Bold,

                        color = darkAwareColor(Color(0xFF1D4ED8), MaterialTheme.colorScheme.primary)

                    )

                }

            }

        }

    }

}



@Composable

private fun b2NoteNavigationItemColors() = NavigationBarItemDefaults.colors(

    selectedIconColor = MaterialTheme.colorScheme.primary,

    selectedTextColor = MaterialTheme.colorScheme.primary,

    indicatorColor = darkAwareColor(Color(0xFFEFF6FF), MaterialTheme.colorScheme.primaryContainer),

    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,

    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant

)



private fun navigateTopLevel(route: String, navController: NavHostController, viewModel: Board2NotesViewModel) {

    when (route) {

        Route.Home -> viewModel.goHome()

        Route.Notes -> viewModel.refreshNotes()

        Route.Favorites -> viewModel.refreshNotes()

        Route.Trash -> viewModel.refreshNotes()

        Route.Archive -> viewModel.refreshNotes()

    }

    navController.navigate(route) {

        popUpTo(navController.graph.startDestinationId) { saveState = false }

        launchSingleTop = true

        restoreState = false

    }

}



@OptIn(ExperimentalFoundationApi::class)

@Composable

private fun HomeScreen(

    state: Board2NotesUiState,

    searchQuery: String,

    selectedNoteIds: Set<String>,

    onSelectionChange: (Set<String>) -> Unit,

    onStart: () -> Unit,

    onOpenNote: (SavedNote) -> Unit,

    onFolderOpen: (String) -> Unit,

    onArchiveNote: (String) -> Unit,

    onDeleteNote: (String) -> Unit,

    sortBy: SortBy,

    viewModel: Board2NotesViewModel

) {

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current

    val screenWidthDp = configuration.screenWidthDp

    val columns = if (screenWidthDp >= 600) 4 else 2



    var pendingDelete by remember { mutableStateOf<SavedNote?>(null) }

    pendingDelete?.let { note ->

        ConfirmDeleteNoteDialog(

            onDismiss = { pendingDelete = null },

            onConfirm = {

                pendingDelete = null

                onDeleteNote(note.id)

            }

        )

    }



    val filteredNotes = remember(state.savedNotes, searchQuery) {

        val q = searchQuery.trim()

        if (q.isBlank()) {

            state.savedNotes.filter { !it.isArchived && !it.isDeleted }

        } else {

            state.savedNotes.filter {

                !it.isArchived && !it.isDeleted && (it.title.contains(q, true) || it.body.contains(q, true) || it.courseName.contains(q, true))

            }

        }

    }



    val sortedNotes = remember(filteredNotes, sortBy) {

        when (sortBy) {

            SortBy.Name -> filteredNotes.sortedBy { it.title.lowercase() }

            SortBy.Date -> filteredNotes.sortedByDescending { it.createdAtEpochMs }

        }

    }



    val courseColorSnapshot = customCourseColors.toMap()

    val existingCourses = remember(state.savedNotes, courseColorSnapshot) {

        val noteCourses = state.savedNotes
            .filter { !it.isDeleted }
            .map { it.courseName.ifBlank { "Genel" } }

        val courses = (noteCourses + courseColorSnapshot.keys)
            .map { it.ifBlank { "Genel" } }
            .distinct()
            .sorted()

        if ("Genel" !in courses) listOf("Genel") + courses else courses

    }



    // Drag and Drop state variables

    var activeDraggedNote by remember { mutableStateOf<SavedNote?>(null) }

    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    var fingerPosition by remember { mutableStateOf(Offset.Zero) }

    var currentHoveredCourse by remember { mutableStateOf<String?>(null) }

    val courseBounds = remember { mutableStateMapOf<String, Rect>() }

    val cardPositions = remember { mutableStateMapOf<String, Rect>() }



    Box(modifier = Modifier.fillMaxSize()) {

        ScreenColumn {

            Spacer(Modifier.height(16.dp))



            // Premium horizontally scrollable categories (dersler) header at top of home screen

            Text(

                text = "Dersleriniz",

                style = MaterialTheme.typography.titleMedium,

                fontWeight = FontWeight.Bold,

                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)

            )

            Spacer(Modifier.height(8.dp))

            Row(

                modifier = Modifier

                    .fillMaxWidth()

                    .horizontalScroll(rememberScrollState())

                    .padding(vertical = 4.dp),

                horizontalArrangement = Arrangement.spacedBy(10.dp)

            ) {

                existingCourses.forEach { courseName ->

                    val isHovered = currentHoveredCourse == courseName

                    val count = state.savedNotes.count { 

                        it.courseName == (if (courseName == "Genel") "" else courseName) && !it.isArchived && !it.isDeleted

                    }

                    val folderColorVal = if (courseName == "Genel") Color(0xFF64748B) else folderColor(courseName)



                    Surface(

                        modifier = Modifier

                            .onGloballyPositioned { coordinates ->

                                courseBounds[courseName] = coordinates.boundsInRoot()

                            }

                            .graphicsLayer {

                                scaleX = if (isHovered) 1.1f else 1.0f

                                scaleY = if (isHovered) 1.1f else 1.0f

                            }

                            .clickable {

                                onFolderOpen(courseName)

                            },

                        shape = RoundedCornerShape(16.dp),

                        color = if (isHovered) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface,

                        border = BorderStroke(

                            width = if (isHovered) 2.dp else 1.dp,

                            color = if (isHovered) MaterialTheme.colorScheme.primary else darkAwareColor(Color(0xFFEFF6FF), MaterialTheme.colorScheme.outlineVariant)

                        ),

                        shadowElevation = if (isHovered) 4.dp else 1.dp

                    ) {

                        Row(

                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),

                            verticalAlignment = Alignment.CenterVertically

                        ) {

                            Surface(

                                shape = RoundedCornerShape(8.dp),

                                color = folderColorVal.copy(alpha = 0.12f),

                                modifier = Modifier.size(32.dp)

                            ) {

                                Box(contentAlignment = Alignment.Center) {

                                    Icon(

                                        imageVector = Icons.Default.Folder,

                                        contentDescription = null,

                                        tint = folderColorVal,

                                        modifier = Modifier.size(16.dp)

                                    )

                                }

                            }

                            Spacer(Modifier.width(10.dp))

                            Column {

                                Text(

                                    text = courseName,

                                    style = MaterialTheme.typography.bodyMedium,

                                    fontWeight = FontWeight.Bold,

                                    color = if (isHovered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

                                )

                                Text(

                                    text = "$count not",

                                    style = MaterialTheme.typography.labelSmall,

                                    color = MaterialTheme.colorScheme.onSurfaceVariant

                                )

                            }

                        }

                    }

                }

            }



            Spacer(Modifier.height(18.dp))



            Text(

                text = if (searchQuery.isNotBlank()) "Arama Sonuçları (${sortedNotes.size})" else "Tüm Notlar",

                style = MaterialTheme.typography.titleMedium,

                fontWeight = FontWeight.Bold,

                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)

            )

            Spacer(Modifier.height(12.dp))



            if (sortedNotes.isEmpty()) {

                HomeEmptyNotesCard()

            } else {

                val chunks = remember(sortedNotes, columns) { sortedNotes.chunked(columns) }

                chunks.forEach { rowItems ->

                    Row(

                        modifier = Modifier.fillMaxWidth(),

                        horizontalArrangement = Arrangement.spacedBy(12.dp)

                    ) {

                        rowItems.forEach { note ->

                            var showContextMenu by remember { mutableStateOf(false) }

                            val isSelected = note.id in selectedNoteIds



                            Box(

                                modifier = Modifier

                                    .weight(1f)

                                    .onGloballyPositioned { coordinates ->

                                        cardPositions[note.id] = coordinates.boundsInRoot()

                                    }

                                    .pointerInput(note, selectedNoteIds) {

                                        detectDragGesturesAfterLongPress(

                                            onDragStart = { offset ->

                                                if (selectedNoteIds.isEmpty()) {

                                                    activeDraggedNote = note

                                                    dragOffset = Offset.Zero

                                                    val cardRect = cardPositions[note.id]

                                                    if (cardRect != null) {

                                                        fingerPosition = Offset(cardRect.left + offset.x, cardRect.top + offset.y)

                                                    }

                                                }

                                            },

                                            onDrag = { change, dragAmount ->

                                                if (activeDraggedNote != null) {

                                                    change.consume()

                                                    dragOffset += dragAmount

                                                    val cardRect = cardPositions[note.id]

                                                    if (cardRect != null) {

                                                        val absoluteTouch = Offset(

                                                            x = cardRect.left + change.position.x,

                                                            y = cardRect.top + change.position.y

                                                        )

                                                        fingerPosition = absoluteTouch

                                                        currentHoveredCourse = courseBounds.entries.firstOrNull { 

                                                            it.value.contains(absoluteTouch) 

                                                        }?.key

                                                    }

                                                }

                                            },

                                            onDragEnd = {

                                                if (activeDraggedNote != null && activeDraggedNote!!.id == note.id) {

                                                    val targetCourse = currentHoveredCourse

                                                    if (targetCourse != null) {

                                                        val finalCourse = if (targetCourse == "Genel") "" else targetCourse

                                                        viewModel.updateNoteCourse(activeDraggedNote!!.id, finalCourse)

                                                    } else {

                                                        showContextMenu = true

                                                    }

                                                    activeDraggedNote = null

                                                    dragOffset = Offset.Zero

                                                    currentHoveredCourse = null

                                                }

                                            },

                                            onDragCancel = {

                                                if (activeDraggedNote != null && activeDraggedNote!!.id == note.id) {

                                                    if (currentHoveredCourse == null) {

                                                        showContextMenu = true

                                                    }

                                                    activeDraggedNote = null

                                                    dragOffset = Offset.Zero

                                                    currentHoveredCourse = null

                                                }

                                            }

                                        )

                                    }

                            ) {

                                Box(

                                    modifier = Modifier.combinedClickable(

                                        onClick = {

                                            if (selectedNoteIds.isNotEmpty()) {

                                                onSelectionChange(

                                                    if (isSelected) selectedNoteIds - note.id else selectedNoteIds + note.id

                                                )

                                            } else {

                                                onOpenNote(note)

                                            }

                                        },

                                        onLongClick = {

                                            if (selectedNoteIds.isEmpty()) {

                                                showContextMenu = true

                                            }

                                        }

                                    )

                                ) {

                                    SamsungNoteCard(

                                        note = note,

                                        isSelected = isSelected,

                                        onClick = {

                                            if (selectedNoteIds.isNotEmpty()) {

                                                onSelectionChange(

                                                    if (isSelected) selectedNoteIds - note.id else selectedNoteIds + note.id

                                                )

                                            } else {

                                                onOpenNote(note)

                                            }

                                        }

                                    )



                                    // High-quality contextual small dropdown menu

                                    DropdownMenu(

                                        expanded = showContextMenu,

                                        onDismissRequest = { showContextMenu = false },

                                        modifier = Modifier

                                            .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp))

                                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))

                                            .width(220.dp),

                                        shape = RoundedCornerShape(16.dp)

                                    ) {

                                        val itemColor = MaterialTheme.colorScheme.onSurface

                                        val isFav = note.isFavorite



                                        DropdownMenuItem(

                                            text = { Text("Aç", fontWeight = FontWeight.Medium, color = itemColor) },

                                            leadingIcon = { Icon(Icons.Default.Edit, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary) },

                                            onClick = {

                                                showContextMenu = false

                                                onOpenNote(note)

                                            }

                                        )

                                        DropdownMenuItem(

                                            text = { Text(if (isFav) "Favorilerden Çıkar" else "Favoriye Ekle", fontWeight = FontWeight.Medium, color = itemColor) },

                                            leadingIcon = {

                                                Icon(

                                                    imageVector = Icons.Default.Star,

                                                    contentDescription = null,

                                                    modifier = Modifier.size(20.dp),

                                                    tint = Color(0xFFF59E0B)

                                                )

                                            },

                                            onClick = {

                                                showContextMenu = false

                                                viewModel.toggleFavorite(note.id)

                                            }

                                        )

                                        DropdownMenuItem(

                                            text = { Text("Arşivle", fontWeight = FontWeight.Medium, color = itemColor) },

                                            leadingIcon = { Icon(Icons.Default.Archive, null, modifier = Modifier.size(20.dp), tint = Color(0xFF1B5E20)) },

                                            onClick = {

                                                showContextMenu = false

                                                onArchiveNote(note.id)

                                            }

                                        )

                                        DropdownMenuItem(

                                            text = { Text("Çoklu Seç", fontWeight = FontWeight.Medium, color = itemColor) },

                                            leadingIcon = { Icon(Icons.Default.Check, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary) },

                                            onClick = {

                                                showContextMenu = false

                                                onSelectionChange(selectedNoteIds + note.id)

                                            }

                                        )

                                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                        DropdownMenuItem(

                                            text = { Text("Sil", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.error) },

                                            leadingIcon = { Icon(Icons.Default.Delete, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error) },

                                            onClick = {

                                                showContextMenu = false

                                                pendingDelete = note

                                            }

                                        )

                                    }

                                }

                            }

                        }

                        val emptySlots = columns - rowItems.size

                        repeat(emptySlots) {

                            Spacer(modifier = Modifier.weight(1f))

                        }

                    }

                    Spacer(Modifier.height(12.dp))

                }

            }

            Spacer(Modifier.height(96.dp))

        }



        // Floating Drag Preview card

        if (activeDraggedNote != null) {

            Box(

                modifier = Modifier

                    .offset {

                        IntOffset(

                            x = (fingerPosition.x - 80.dp.toPx()).roundToInt(),

                            y = (fingerPosition.y - 60.dp.toPx()).roundToInt()

                        )

                    }

                    .graphicsLayer {

                        alpha = 0.85f

                        scaleX = 1.05f

                        scaleY = 1.05f

                        shadowElevation = 8.dp.toPx()

                    }

                    .width(160.dp)

            ) {

                SamsungNoteCard(note = activeDraggedNote!!, onClick = {})

            }

        }

    }

}



@Composable

private fun SamsungNoteCard(

    note: SavedNote,

    isSelected: Boolean = false,

    isHovered: Boolean = false,

    onClick: () -> Unit

) {

    val previewPath = note.previewImagePath()

    val bitmap = remember(previewPath) {

        previewPath?.let { path ->

            try {

                val options = BitmapFactory.Options().apply {

                    inSampleSize = 4

                }

                BitmapFactory.decodeFile(path, options)

            } catch (e: Exception) {

                null

            }

        }

    }



    Surface(

        modifier = Modifier

            .fillMaxWidth()

            .clickable(onClick = onClick),

        shape = RoundedCornerShape(18.dp),

        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface,

        tonalElevation = if (isSelected) 3.dp else 1.dp,

        border = BorderStroke(

            width = if (isSelected) 2.5.dp else 1.dp,

            color = if (isSelected) MaterialTheme.colorScheme.primary else darkAwareColor(Color(0xFFEFF6FF), MaterialTheme.colorScheme.outlineVariant)

        )

    ) {

        Column(modifier = Modifier.fillMaxWidth()) {

            Box(

                modifier = Modifier

                    .fillMaxWidth()

                    .height(90.dp)

                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))

            ) {

                if (bitmap != null) {

                    Image(

                        bitmap = bitmap.asImageBitmap(),

                        contentDescription = null,

                        contentScale = ContentScale.Crop,

                        modifier = Modifier.fillMaxSize()

                    )

                } else if (note.noteType == NoteType.Text && note.preview.isNotBlank()) {

                    Box(

                        modifier = Modifier

                            .fillMaxSize()

                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))

                            .padding(12.dp)

                    ) {

                        Text(

                            text = note.preview,

                            fontSize = 13.sp,

                            lineHeight = 18.sp,

                            fontWeight = FontWeight.Medium,

                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),

                            maxLines = 4,

                            overflow = TextOverflow.Ellipsis,

                            modifier = Modifier.fillMaxSize()

                        )

                    }

                } else {

                    val isCanvas = note.noteType == NoteType.Canvas

                    val gradientColors = if (isCanvas) {

                        listOf(Color(0xFFEFF6FF), Color(0xFFDBEAFE))

                    } else {

                        listOf(Color(0xFFF8FAFC), Color(0xFFF1F5F9))

                    }

                    val gradient = Brush.linearGradient(colors = gradientColors)



                    Box(

                        modifier = Modifier

                            .fillMaxSize()

                            .background(gradient),

                        contentAlignment = Alignment.Center

                    ) {

                        Icon(

                            imageVector = if (isCanvas) Icons.Default.Brush else Icons.Default.TextFields,

                            contentDescription = null,

                            tint = if (isCanvas) Color(0xFF3B82F6).copy(alpha = 0.25f) else Color(0xFF64748B).copy(alpha = 0.25f),

                            modifier = Modifier.size(32.dp)

                        )

                    }

                }



                if (isSelected) {

                    Surface(

                        shape = CircleShape,

                        color = MaterialTheme.colorScheme.primary,

                        modifier = Modifier

                            .padding(8.dp)

                            .size(24.dp)

                            .align(Alignment.TopEnd)

                    ) {

                        Box(contentAlignment = Alignment.Center) {

                            Icon(

                                imageVector = Icons.Default.Check,

                                contentDescription = "Seçildi",

                                tint = Color.White,

                                modifier = Modifier.size(16.dp)

                            )

                        }

                    }

                }

            }

            Column(modifier = Modifier.padding(12.dp)) {

                if (note.courseName.isNotBlank()) {

                    val badgeColor = folderColor(note.courseName)

                    Surface(

                        shape = RoundedCornerShape(8.dp),

                        color = badgeColor.copy(alpha = 0.12f),

                        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.25f)),

                        modifier = Modifier.padding(bottom = 6.dp)

                    ) {

                        Text(

                            text = note.courseName,

                            style = MaterialTheme.typography.labelSmall,

                            fontWeight = FontWeight.Bold,

                            color = badgeColor,

                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),

                            maxLines = 1,

                            overflow = TextOverflow.Ellipsis

                        )

                    }

                }



                Text(

                    text = note.title.ifBlank { "Başlıksız Not" },

                    style = MaterialTheme.typography.titleSmall,

                    fontWeight = FontWeight.Bold,

                    color = MaterialTheme.colorScheme.onSurface,

                    maxLines = 1,

                    overflow = TextOverflow.Ellipsis

                )

                Spacer(Modifier.height(4.dp))

                Text(

                    text = note.preview.ifBlank { "Not içeriği boş." },

                    style = MaterialTheme.typography.bodySmall,

                    color = MaterialTheme.colorScheme.onSurfaceVariant,

                    maxLines = 2,

                    overflow = TextOverflow.Ellipsis,

                    lineHeight = 16.sp

                )

            }

        }

    }

}



@Composable

private fun SamsungNoteCard(note: SavedNote, onClick: () -> Unit) {

    val previewPath = note.previewImagePath()

    val bitmap = remember(previewPath) {

        previewPath?.let { path ->

            try {

                val options = BitmapFactory.Options().apply {

                    inSampleSize = 4

                }

                BitmapFactory.decodeFile(path, options)

            } catch (e: Exception) {

                null

            }

        }

    }



    Surface(

        modifier = Modifier

            .fillMaxWidth()

            .clickable(onClick = onClick),

        shape = RoundedCornerShape(18.dp),

        color = MaterialTheme.colorScheme.surface,

        tonalElevation = 1.dp,

        border = BorderStroke(1.dp, darkAwareColor(Color(0xFFE2E8F0), MaterialTheme.colorScheme.outlineVariant))

    ) {

        Column(modifier = Modifier.fillMaxWidth()) {

            Box(

                modifier = Modifier

                    .fillMaxWidth()

                    .height(90.dp)

                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))

            ) {

                if (bitmap != null) {

                    Image(

                        bitmap = bitmap.asImageBitmap(),

                        contentDescription = null,

                        contentScale = ContentScale.Crop,

                        modifier = Modifier.fillMaxSize()

                    )

                } else if (note.noteType == NoteType.Text && note.preview.isNotBlank()) {

                    Box(

                        modifier = Modifier

                            .fillMaxSize()

                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))

                            .padding(12.dp)

                    ) {

                        Text(

                            text = note.preview,

                            fontSize = 13.sp,

                            lineHeight = 18.sp,

                            fontWeight = FontWeight.Medium,

                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),

                            maxLines = 4,

                            overflow = TextOverflow.Ellipsis,

                            modifier = Modifier.fillMaxSize()

                        )

                    }

                } else {

                    val isCanvas = note.noteType == NoteType.Canvas

                    val gradientColors = if (isCanvas) {

                        listOf(Color(0xFFEFF6FF), Color(0xFFDBEAFE))

                    } else {

                        listOf(Color(0xFFF8FAFC), Color(0xFFF1F5F9))

                    }

                    val gradient = Brush.linearGradient(colors = gradientColors)



                    Box(

                        modifier = Modifier

                            .fillMaxSize()

                            .background(gradient),

                        contentAlignment = Alignment.Center

                    ) {

                        Icon(

                            imageVector = if (isCanvas) Icons.Default.Brush else Icons.Default.TextFields,

                            contentDescription = null,

                            tint = if (isCanvas) Color(0xFF3B82F6).copy(alpha = 0.25f) else Color(0xFF64748B).copy(alpha = 0.25f),

                            modifier = Modifier.size(32.dp)

                        )

                    }

                }

            }

            Column(modifier = Modifier.padding(12.dp)) {

                if (note.courseName.isNotBlank()) {

                    val badgeColor = folderColor(note.courseName)

                    Surface(

                        shape = RoundedCornerShape(8.dp),

                        color = badgeColor.copy(alpha = 0.12f),

                        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.25f)),

                        modifier = Modifier.padding(bottom = 6.dp)

                    ) {

                        Text(

                            text = note.courseName,

                            style = MaterialTheme.typography.labelSmall,

                            fontWeight = FontWeight.Bold,

                            color = badgeColor,

                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),

                            maxLines = 1,

                            overflow = TextOverflow.Ellipsis

                        )

                    }

                }



                Text(

                    text = note.title.ifBlank { "Başlıksız Not" },

                    style = MaterialTheme.typography.titleSmall,

                    fontWeight = FontWeight.Bold,

                    color = MaterialTheme.colorScheme.onSurface,

                    maxLines = 1,

                    overflow = TextOverflow.Ellipsis

                )

                Spacer(Modifier.height(4.dp))

                Text(

                    text = note.preview.ifBlank { "Not içeriği boş." },

                    style = MaterialTheme.typography.bodySmall,

                    color = MaterialTheme.colorScheme.onSurfaceVariant,

                    maxLines = 2,

                    overflow = TextOverflow.Ellipsis,

                    lineHeight = 16.sp

                )

                Spacer(Modifier.height(8.dp))

                Text(

                    text = formatDate(note.updatedAtEpochMs),

                    style = MaterialTheme.typography.labelSmall,

                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)

                )

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

        border = BorderStroke(1.dp, darkAwareColor(Color(0xFFE2E8F0), MaterialTheme.colorScheme.outlineVariant))

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

private fun SwipeableNoteActions(

    onArchive: () -> Unit,

    onDelete: () -> Unit,

    isVertical: Boolean = true,

    content: @Composable () -> Unit

) {

    val coroutineScope = rememberCoroutineScope()

    val density = LocalDensity.current

    var cardWidthPx by remember { mutableStateOf(0f) }

    val actionsWidthPx = cardWidthPx / 2f

    val actionsWidthDp = with(density) { actionsWidthPx.toDp() }

    val swipeOffset = remember { Animatable(0f) }



    Box(

        modifier = Modifier

            .fillMaxWidth()

            .clip(RoundedCornerShape(18.dp))

            .background(darkAwareColor(Color(0xFFF1F5F9), MaterialTheme.colorScheme.surfaceVariant))

            .height(IntrinsicSize.Max)

            .onSizeChanged { size ->

                cardWidthPx = size.width.toFloat()

            }

    ) {

        if (isVertical) {

            Column(

                modifier = Modifier

                    .align(Alignment.CenterEnd)

                    .width(actionsWidthDp)

                    .fillMaxHeight(),

                verticalArrangement = Arrangement.Center

            ) {

                Box(

                    modifier = Modifier

                        .weight(1f)

                        .fillMaxWidth()

                        .background(Color(0xFF3B82F6))

                        .clickable {

                            coroutineScope.launch {

                                swipeOffset.animateTo(0f)

                            }

                            onArchive()

                        },

                    contentAlignment = Alignment.Center

                ) {

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {

                        Icon(

                            imageVector = Icons.Default.Folder,

                            contentDescription = "Arşivle",

                            tint = Color.White,

                            modifier = Modifier.size(20.dp)

                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(

                            text = "Arşivle",

                            style = MaterialTheme.typography.labelSmall,

                            color = Color.White,

                            fontWeight = FontWeight.Bold

                        )

                    }

                }



                Box(

                    modifier = Modifier

                        .weight(1f)

                        .fillMaxWidth()

                        .background(Color(0xFFEF4444))

                        .clickable {

                            coroutineScope.launch {

                                swipeOffset.animateTo(0f)

                            }

                            onDelete()

                        },

                    contentAlignment = Alignment.Center

                ) {

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {

                        Icon(

                            imageVector = Icons.Default.Delete,

                            contentDescription = "Sil",

                            tint = Color.White,

                            modifier = Modifier.size(20.dp)

                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(

                            text = "Sil",

                            style = MaterialTheme.typography.labelSmall,

                            color = Color.White,

                            fontWeight = FontWeight.Bold

                        )

                    }

                }

            }

        } else {

            Row(

                modifier = Modifier

                    .align(Alignment.CenterEnd)

                    .width(actionsWidthDp)

                    .fillMaxHeight(),

                verticalAlignment = Alignment.CenterVertically

            ) {

                Box(

                    modifier = Modifier

                        .weight(1f)

                        .fillMaxHeight()

                        .background(Color(0xFF3B82F6))

                        .clickable {

                            coroutineScope.launch {

                                swipeOffset.animateTo(0f)

                            }

                            onArchive()

                        },

                    contentAlignment = Alignment.Center

                ) {

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {

                        Icon(

                            imageVector = Icons.Default.Folder,

                            contentDescription = "Arşivle",

                            tint = Color.White,

                            modifier = Modifier.size(20.dp)

                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(

                            text = "Arşivle",

                            style = MaterialTheme.typography.labelSmall,

                            color = Color.White,

                            fontWeight = FontWeight.Bold

                        )

                    }

                }



                Box(

                    modifier = Modifier

                        .weight(1f)

                        .fillMaxHeight()

                        .background(Color(0xFFEF4444))

                        .clickable {

                            coroutineScope.launch {

                                swipeOffset.animateTo(0f)

                            }

                            onDelete()

                        },

                    contentAlignment = Alignment.Center

                ) {

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {

                        Icon(

                            imageVector = Icons.Default.Delete,

                            contentDescription = "Sil",

                            tint = Color.White,

                            modifier = Modifier.size(20.dp)

                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(

                            text = "Sil",

                            style = MaterialTheme.typography.labelSmall,

                            color = Color.White,

                            fontWeight = FontWeight.Bold

                        )

                    }

                }

            }

        }



        Box(

            modifier = Modifier

                .fillMaxWidth()

                .offset { IntOffset(swipeOffset.value.roundToInt(), 0) }

                .pointerInput(actionsWidthPx) {

                    detectHorizontalDragGestures(

                        onHorizontalDrag = { change, dragAmount ->

                            change.consume()

                            val targetOffset = swipeOffset.value + dragAmount

                            coroutineScope.launch {

                                swipeOffset.snapTo(targetOffset.coerceIn(-actionsWidthPx, 0f))

                            }

                        },

                        onDragEnd = {

                            coroutineScope.launch {

                                if (swipeOffset.value < -actionsWidthPx / 2f) {

                                    swipeOffset.animateTo(-actionsWidthPx)

                                } else {

                                    swipeOffset.animateTo(0f)

                                }

                            }

                        }

                    )

                }

                .background(MaterialTheme.colorScheme.surface)

        ) {

            content()

        }

    }

}



@Composable

private fun ConfirmDeleteNoteDialog(

    onDismiss: () -> Unit,

    onConfirm: () -> Unit,

    title: String = "Notu silmek istediğinize emin misiniz?",

    confirmText: String = "Sil"

) {

    AlertDialog(

        onDismissRequest = onDismiss,

        shape = RoundedCornerShape(24.dp),

        containerColor = MaterialTheme.colorScheme.surface,

        title = { Text(title, fontWeight = FontWeight.Bold) },

        text = { Text("Bu not çöp kutusuna taşınacak. Daha sonra geri yükleyebilirsiniz.") },

        confirmButton = {

            Button(

                onClick = onConfirm,

                shape = RoundedCornerShape(14.dp),

                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)

            ) {

                Text(confirmText)

            }

        },

        dismissButton = {

            TextButton(onClick = onDismiss) {

                Text("Vazgeç", color = MaterialTheme.colorScheme.secondary)

            }

        }

    )

}



@Composable

private fun CourseSelectorDialog(

    currentCourse: String,

    existingCourses: List<String>,

    onSelect: (String) -> Unit,

    onAddNewCourse: () -> Unit,

    onDismiss: () -> Unit

) {

    AlertDialog(

        onDismissRequest = onDismiss,

        shape = RoundedCornerShape(26.dp),

        containerColor = MaterialTheme.colorScheme.surface,

        title = {

            Row(

                verticalAlignment = Alignment.CenterVertically,

                horizontalArrangement = Arrangement.SpaceBetween,

                modifier = Modifier.fillMaxWidth()

            ) {

                Text(

                    text = "Ders Seçin",

                    fontWeight = FontWeight.Bold,

                    style = MaterialTheme.typography.titleLarge,

                    color = MaterialTheme.colorScheme.onSurface

                )

                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {

                    Icon(

                        imageVector = Icons.Default.Close,

                        contentDescription = "Kapat",

                        tint = MaterialTheme.colorScheme.onSurfaceVariant

                    )

                }

            }

        },

        text = {

            Column(

                modifier = Modifier

                    .fillMaxWidth()

                    .heightIn(max = 300.dp)

                    .verticalScroll(rememberScrollState())

            ) {

                // Option: Genel

                val isGenelSelected = currentCourse.isBlank()

                Surface(

                    onClick = { onSelect("") },

                    shape = RoundedCornerShape(16.dp),

                    color = if (isGenelSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent,

                    border = BorderStroke(

                        width = 1.dp,

                        color = if (isGenelSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)

                    ),

                    modifier = Modifier

                        .fillMaxWidth()

                        .padding(vertical = 4.dp)

                ) {

                    Row(

                        modifier = Modifier

                            .fillMaxWidth()

                            .padding(horizontal = 16.dp, vertical = 12.dp),

                        verticalAlignment = Alignment.CenterVertically

                    ) {

                        Surface(

                            shape = RoundedCornerShape(10.dp),

                            color = Color(0xFF64748B).copy(alpha = 0.12f),

                            modifier = Modifier.size(36.dp)

                        ) {

                            Box(contentAlignment = Alignment.Center) {

                                Icon(

                                    imageVector = Icons.Default.Folder,

                                    contentDescription = null,

                                    tint = Color(0xFF64748B),

                                    modifier = Modifier.size(18.dp)

                                )

                            }

                        }

                        Spacer(Modifier.width(12.dp))

                        Text(

                            text = "Genel",

                            style = MaterialTheme.typography.bodyLarge,

                            fontWeight = if (isGenelSelected) FontWeight.Bold else FontWeight.Normal,

                            color = if (isGenelSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,

                            modifier = Modifier.weight(1f)

                        )

                        if (isGenelSelected) {

                            Icon(

                                imageVector = Icons.Default.Check,

                                contentDescription = "Seçili",

                                tint = MaterialTheme.colorScheme.primary,

                                modifier = Modifier.size(20.dp)

                            )

                        }

                    }

                }



                // Existing courses list

                existingCourses.forEach { courseName ->

                    val isSelected = currentCourse == courseName

                    val color = folderColor(courseName)

                    Surface(

                        onClick = { onSelect(courseName) },

                        shape = RoundedCornerShape(16.dp),

                        color = if (isSelected) color.copy(alpha = 0.1f) else Color.Transparent,

                        border = BorderStroke(

                            width = 1.dp,

                            color = if (isSelected) color.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)

                        ),

                        modifier = Modifier

                            .fillMaxWidth()

                            .padding(vertical = 4.dp)

                    ) {

                        Row(

                            modifier = Modifier

                                .fillMaxWidth()

                                .padding(horizontal = 16.dp, vertical = 12.dp),

                            verticalAlignment = Alignment.CenterVertically

                        ) {

                            Surface(

                                shape = RoundedCornerShape(10.dp),

                                color = color.copy(alpha = 0.12f),

                                modifier = Modifier.size(36.dp)

                            ) {

                                Box(contentAlignment = Alignment.Center) {

                                    Icon(

                                        imageVector = Icons.Default.Folder,

                                        contentDescription = null,

                                        tint = color,

                                        modifier = Modifier.size(18.dp)

                                    )

                                }

                            }

                            Spacer(Modifier.width(12.dp))

                            Text(

                                text = courseName,

                                style = MaterialTheme.typography.bodyLarge,

                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,

                                color = if (isSelected) color else MaterialTheme.colorScheme.onSurface,

                                modifier = Modifier.weight(1f)

                            )

                            if (isSelected) {

                                Icon(

                                    imageVector = Icons.Default.Check,

                                    contentDescription = "Seçili",

                                    tint = color,

                                    modifier = Modifier.size(20.dp)

                                )

                            }

                        }

                    }

                }

            }

        },

        confirmButton = {

            Button(

                onClick = onAddNewCourse,

                shape = RoundedCornerShape(14.dp),

                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),

                modifier = Modifier.fillMaxWidth()

            ) {

                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))

                Spacer(Modifier.width(6.dp))

                Text("Yeni Ders Ekle", fontWeight = FontWeight.Bold)

            }

        }

    )

}



@Composable

private fun SamsungCaptureScreen(onPick: () -> Unit, onCamera: () -> Unit) {

    Column(

        modifier = Modifier

            .fillMaxSize()

            .background(darkAwareColor(Color.White, MaterialTheme.colorScheme.background))

            .padding(horizontal = 24.dp)

            .verticalScroll(rememberScrollState())

    ) {

        Spacer(Modifier.height(16.dp))

        Text(

            "Tahtadan Not Ekle",

            style = MaterialTheme.typography.headlineMedium,

            fontWeight = FontWeight.Bold,

            color = MaterialTheme.colorScheme.onSurface

        )

        Spacer(Modifier.height(6.dp))

        Text(

            "Beyaz tahta veya slayt ekranını çekerek otomatik ders notuna dönüştürün.",

            style = MaterialTheme.typography.bodyMedium,

            color = MaterialTheme.colorScheme.onSurfaceVariant,

            lineHeight = 22.sp

        )

        Spacer(Modifier.height(20.dp))



        // Beautiful Workflow Card

        Surface(

            modifier = Modifier.fillMaxWidth(),

            shape = RoundedCornerShape(20.dp),

            color = darkAwareColor(Color(0xFFF8FAFC), MaterialTheme.colorScheme.surfaceVariant),

            border = BorderStroke(1.dp, darkAwareColor(Color(0xFFEFF6FF), MaterialTheme.colorScheme.outlineVariant))

        ) {

            Column(modifier = Modifier.padding(18.dp)) {

                Text(

                    "Nasıl Çalışır?",

                    style = MaterialTheme.typography.titleMedium,

                    fontWeight = FontWeight.Bold,

                    color = MaterialTheme.colorScheme.primary

                )

                Spacer(Modifier.height(12.dp))

                WorkflowStepRow("1", "Fotoğraf Çek / Yükle", "Sınıf tahtası veya projeksiyon ekranını net bir açıdan çekin.")

                Spacer(Modifier.height(10.dp))

                WorkflowStepRow("2", "Perspektif Köşeleri Belirle", "Otomatik tespit edilen köşeleri elle hassas şekilde düzenleyin.")

                Spacer(Modifier.height(10.dp))

                WorkflowStepRow("3", "AI İyileştirme ve OCR", "Görüntü arka planı temizlenir, metinler taranır ve notunuza aktarılır.")

            }

        }



        Spacer(Modifier.height(18.dp))

        

        MinimalBoardMockCard(

            modifier = Modifier

                .fillMaxWidth()

                .height(200.dp)

        )

        

        Spacer(Modifier.height(24.dp))

        

        Row(

            modifier = Modifier

                .fillMaxWidth()

                .padding(bottom = 32.dp),

            horizontalArrangement = Arrangement.spacedBy(14.dp)

        ) {

            OutlinedButton(

                onClick = onPick,

                modifier = Modifier

                    .weight(1f)

                    .height(56.dp),

                shape = RoundedCornerShape(18.dp),

                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)

            ) {

                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(20.dp))

                Spacer(Modifier.width(8.dp))

                Text("Galeriden Seç", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)

            }

            

            Button(

                onClick = onCamera,

                modifier = Modifier

                    .weight(1f)

                    .height(56.dp),

                shape = RoundedCornerShape(18.dp),

                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)

            ) {

                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp))

                Spacer(Modifier.width(8.dp))

                Text("Fotoğraf Çek", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)

            }

        }

    }

}



@Composable

private fun WorkflowStepRow(stepNum: String, title: String, desc: String) {

    Row(verticalAlignment = Alignment.Top) {

        Surface(

            shape = CircleShape,

            color = MaterialTheme.colorScheme.primaryContainer,

            modifier = Modifier.size(26.dp)

        ) {

            Box(contentAlignment = Alignment.Center) {

                Text(stepNum, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            }

        }

        Spacer(Modifier.width(12.dp))

        Column {

            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        }

    }

}



@Composable

private fun SamsungImageReviewScreen(

    state: Board2NotesUiState,

    viewModel: Board2NotesViewModel,

    onContinue: () -> Unit

) {

    val addingToNote = state.pendingScanNoteId != null

    Column(

        modifier = Modifier

            .fillMaxSize()

            .background(darkAwareColor(Color.White, MaterialTheme.colorScheme.background))

    ) {

        Column(

            modifier = Modifier

                .weight(1f)

                .padding(horizontal = 18.dp)

        ) {

            Spacer(Modifier.height(12.dp))

            Text(

                if (addingToNote) "Tahtayı nota ekle" else "Görseli kontrol et",

                style = MaterialTheme.typography.titleLarge,

                fontWeight = FontWeight.SemiBold,

                color = MaterialTheme.colorScheme.onSurface

            )

            Spacer(Modifier.height(6.dp))

            Text(

                if (addingToNote) "Backend çıktısı bu açık nota eklenecek." else "Gerekirse döndür, sonra devam et.",

                style = MaterialTheme.typography.bodyMedium,

                color = MaterialTheme.colorScheme.onSurfaceVariant

            )

            Spacer(Modifier.height(18.dp))

            Surface(

                modifier = Modifier

                    .fillMaxWidth()

                    .weight(1f),

                shape = RoundedCornerShape(22.dp),

                color = darkAwareColor(Color(0xFFF8FAFC), MaterialTheme.colorScheme.surfaceVariant),

                border = BorderStroke(1.dp, darkAwareColor(Color(0xFFE2E8F0), MaterialTheme.colorScheme.outlineVariant))

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

            Spacer(Modifier.height(14.dp))

        }

        Surface(

            modifier = Modifier.fillMaxWidth(),

            color = Color.White,

            shadowElevation = 8.dp,

            tonalElevation = 0.dp

        ) {

            Row(

                horizontalArrangement = Arrangement.spacedBy(12.dp),

                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)

            ) {

                OutlinedButton(

                    onClick = { viewModel.rotateSelected(-90f) },

                    modifier = Modifier

                        .weight(1f)

                        .height(50.dp),

                    shape = RoundedCornerShape(16.dp)

                ) {

                    Icon(Icons.Default.Rotate90DegreesCcw, contentDescription = null, modifier = Modifier.size(18.dp))

                    Spacer(Modifier.width(8.dp))

                    Text("Döndür")

                }

                Button(

                    onClick = onContinue,

                    modifier = Modifier

                        .weight(1f)

                        .height(50.dp),

                    enabled = state.selectedImage != null,

                    shape = RoundedCornerShape(16.dp)

                ) {

                    Icon(if (addingToNote) Icons.Default.Add else Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))

                    Spacer(Modifier.width(8.dp))

                    Text(if (addingToNote) "Tahtayı ekle" else "Devam et")

                }

            }

        }

    }

}



@Composable

private fun MinimalBoardMockCard(modifier: Modifier = Modifier) {

    Surface(

        modifier = modifier,

        shape = RoundedCornerShape(24.dp),

        color = darkAwareColor(Color(0xFFF8FAFC), MaterialTheme.colorScheme.surfaceVariant),

        border = BorderStroke(1.dp, darkAwareColor(Color(0xFFE2E8F0), MaterialTheme.colorScheme.outlineVariant))

    ) {

        Canvas(modifier = Modifier.fillMaxSize().padding(24.dp)) {

            val boardLeft = size.width * 0.08f

            val boardTop = size.height * 0.22f

            val boardWidth = size.width * 0.84f

            val boardHeight = size.height * 0.46f

            drawRoundRect(

                color = Color.White,

                topLeft = Offset(boardLeft, boardTop),

                size = Size(boardWidth, boardHeight),

                cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx())

            )

            drawRoundRect(

                color = Color(0xFFCBD5E1),

                topLeft = Offset(boardLeft, boardTop),

                size = Size(boardWidth, boardHeight),

                cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx()),

                style = Stroke(width = 2.dp.toPx())

            )

            listOf(0.18f, 0.34f, 0.50f, 0.66f).forEachIndexed { index, y ->

                val end = if (index % 2 == 0) 0.72f else 0.58f

                drawLine(

                    color = if (index == 0) Color(0xFF2563EB) else Color(0xFF0F172A),

                    start = Offset(boardLeft + boardWidth * 0.16f, boardTop + boardHeight * y),

                    end = Offset(boardLeft + boardWidth * end, boardTop + boardHeight * y),

                    strokeWidth = 3.dp.toPx(),

                    cap = StrokeCap.Round

                )

            }

            drawRoundRect(

                color = Color(0xFF2563EB),

                topLeft = Offset(boardLeft - 10.dp.toPx(), boardTop - 10.dp.toPx()),

                size = Size(boardWidth + 20.dp.toPx(), boardHeight + 20.dp.toPx()),

                cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx()),

                style = Stroke(width = 2.dp.toPx())

            )

        }

    }

}



@Composable

private fun CaptureScreen(onPick: () -> Unit, onCamera: () -> Unit) {

    Column(

        modifier = Modifier

            .fillMaxSize()

            .background(Color(0xFF0F172A))

            .padding(horizontal = 24.dp)

    ) {

        Spacer(Modifier.height(28.dp))



        Text(

            "Tahta Tara",

            style = MaterialTheme.typography.headlineMedium,

            fontWeight = FontWeight.Bold,

            color = Color.White,

            letterSpacing = 0.5.sp

        )

        Spacer(Modifier.height(8.dp))

        Text(

            "En iyi sonuçlar için tahta köşelerinin kadraja sığdığından emin olun.",

            style = MaterialTheme.typography.bodyMedium,

            color = Color.White.copy(alpha = 0.5f),

            lineHeight = 20.sp

        )



        Spacer(Modifier.height(28.dp))



        CapturePreviewCard(

            modifier = Modifier

                .fillMaxWidth()

                .weight(1f)

        )



        Spacer(Modifier.height(32.dp))



        // Professional Camera Shutter layout

        Row(

            modifier = Modifier

                .fillMaxWidth()

                .padding(bottom = 32.dp),

            horizontalArrangement = Arrangement.SpaceEvenly,

            verticalAlignment = Alignment.CenterVertically

        ) {

            // Gallery selection

            Surface(

                onClick = onPick,

                modifier = Modifier.size(54.dp),

                shape = CircleShape,

                color = Color.White.copy(alpha = 0.08f),

                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))

            ) {

                Box(contentAlignment = Alignment.Center) {

                    Icon(

                        Icons.Default.PhotoLibrary,

                        contentDescription = "Galeriden Seç",

                        tint = Color.White,

                        modifier = Modifier.size(22.dp)

                    )

                }

            }



            // Big White Camera Shutter

            Surface(

                onClick = onCamera,

                modifier = Modifier

                    .size(76.dp)

                    .shadow(12.dp, CircleShape, spotColor = Color(0x7F2563EB)),

                shape = CircleShape,

                color = Color.White.copy(alpha = 0.15f)

            ) {

                Box(

                    modifier = Modifier

                        .fillMaxSize()

                        .padding(6.dp)

                        .background(Color.White, CircleShape),

                    contentAlignment = Alignment.Center

                ) {

                    Icon(

                        Icons.Default.CameraAlt,

                        contentDescription = "Fotoğraf Çek",

                        tint = Color(0xFF0F172A),

                        modifier = Modifier.size(26.dp)

                    )

                }

            }



            // Dummy flash toggle

            Surface(

                onClick = { /* Flaş kontrolü */ },

                modifier = Modifier.size(54.dp),

                shape = CircleShape,

                color = Color.White.copy(alpha = 0.08f),

                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))

            ) {

                Box(contentAlignment = Alignment.Center) {

                    Icon(

                        Icons.Default.FlashOn,

                        contentDescription = "Flaş Otomatik",

                        tint = Color.White.copy(alpha = 0.6f),

                        modifier = Modifier.size(22.dp)

                    )

                }

            }

        }

    }

}



@Composable

private fun ImageReviewScreen(state: Board2NotesUiState, viewModel: Board2NotesViewModel, onContinue: () -> Unit) {

    val addingToNote = state.pendingScanNoteId != null

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

            SectionTitle(

                "Görüntüyü kontrol et",

                if (addingToNote) "Bu görüntü açık nota eklenecek." else "Gerekirse döndür, sonra devam et."

            )

            Spacer(Modifier.height(16.dp))

            Surface(

                modifier = Modifier

                    .fillMaxWidth()

                    .weight(1f),

                shape = RoundedCornerShape(22.dp),

                color = MaterialTheme.colorScheme.surface,

                tonalElevation = 1.dp,

                shadowElevation = 2.dp,

                border = BorderStroke(1.dp, darkAwareColor(Color(0xFFE2E8F0), MaterialTheme.colorScheme.outlineVariant))

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

                Icon(if (addingToNote) Icons.Default.Add else Icons.Default.Check, contentDescription = null)

                    Spacer(Modifier.width(6.dp))

                    Text(if (addingToNote) "Nota Ekle" else "Devam Et")

                }

            }

        }

    }

}



@Composable

private fun CapturePreviewCard(modifier: Modifier = Modifier) {

    val infiniteTransition = rememberInfiniteTransition(label = "ViewfinderPulse")

    val borderAlpha by infiniteTransition.animateFloat(

        initialValue = 0.4f,

        targetValue = 0.9f,

        animationSpec = infiniteRepeatable(

            animation = tween(durationMillis = 1500, easing = LinearEasing),

            repeatMode = RepeatMode.Reverse

        ),

        label = "BorderAlpha"

    )



    Surface(

        modifier = modifier,

        shape = RoundedCornerShape(28.dp),

        color = Color(0xFF1E293B),

        shadowElevation = 6.dp,

        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))

    ) {

        Box(

            modifier = Modifier

                .fillMaxSize()

                .background(

                    Brush.verticalGradient(

                        listOf(Color(0xFF1E293B), Color(0xFF0F172A))

                    )

                )

                .padding(24.dp)

        ) {

            Canvas(modifier = Modifier.fillMaxSize()) {

                val stroke = 3.dp.toPx()

                val line = 24.dp.toPx()

                val corner = 8.dp.toPx()



                // Draw a beautiful classroom whiteboard mockup

                val boardWidth = size.width * 0.82f

                val boardHeight = size.height * 0.52f

                val boardLeft = (size.width - boardWidth) / 2f

                val boardTop = (size.height - boardHeight) / 2.3f



                // Whiteboard Shadow

                drawRoundRect(

                    color = Color.Black.copy(alpha = 0.25f),

                    topLeft = Offset(boardLeft + 4.dp.toPx(), boardTop + 4.dp.toPx()),

                    size = Size(boardWidth, boardHeight),

                    cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())

                )



                // Whiteboard Frame (Metallic Silver)

                drawRoundRect(

                    color = Color(0xFF94A3B8),

                    topLeft = Offset(boardLeft, boardTop),

                    size = Size(boardWidth, boardHeight),

                    cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),

                    style = Stroke(width = 4.dp.toPx())

                )



                // Whiteboard Surface

                drawRoundRect(

                    color = Color(0xFFF8FAFC),

                    topLeft = Offset(boardLeft + 2.dp.toPx(), boardTop + 2.dp.toPx()),

                    size = Size(boardWidth - 4.dp.toPx(), boardHeight - 4.dp.toPx()),

                    cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())

                )



                // Draw some realistic whiteboard mock content (marker writing lines)

                // Header line

                drawLine(

                    color = Color(0xFF1E40AF),

                    start = Offset(boardLeft + boardWidth * 0.15f, boardTop + boardHeight * 0.2f),

                    end = Offset(boardLeft + boardWidth * 0.45f, boardTop + boardHeight * 0.2f),

                    strokeWidth = 3.dp.toPx(),

                    cap = StrokeCap.Round

                )

                // Formula or text lines

                drawLine(

                    color = Color(0xFF0F172A),

                    start = Offset(boardLeft + boardWidth * 0.15f, boardTop + boardHeight * 0.38f),

                    end = Offset(boardLeft + boardWidth * 0.75f, boardTop + boardHeight * 0.38f),

                    strokeWidth = 2.5.dp.toPx(),

                    cap = StrokeCap.Round

                )

                drawLine(

                    color = Color(0xFF0F172A),

                    start = Offset(boardLeft + boardWidth * 0.15f, boardTop + boardHeight * 0.52f),

                    end = Offset(boardLeft + boardWidth * 0.65f, boardTop + boardHeight * 0.52f),

                    strokeWidth = 2.5.dp.toPx(),

                    cap = StrokeCap.Round

                )

                // Drawing mark

                drawLine(

                    color = Color(0xFF2563EB),

                    start = Offset(boardLeft + boardWidth * 0.15f, boardTop + boardHeight * 0.7f),

                    end = Offset(boardLeft + boardWidth * 0.5f, boardTop + boardHeight * 0.7f),

                    strokeWidth = 2.5.dp.toPx(),

                    cap = StrokeCap.Round

                )



                // Auto-detect crop frame borders (pulsing/breathing effect)

                val detectLeft = boardLeft - 8.dp.toPx()

                val detectTop = boardTop - 8.dp.toPx()

                val detectWidth = boardWidth + 16.dp.toPx()

                val detectHeight = boardHeight + 16.dp.toPx()



                // Corner L-brackets

                listOf(

                    Offset(detectLeft, detectTop),

                    Offset(detectLeft + detectWidth, detectTop),

                    Offset(detectLeft, detectTop + detectHeight),

                    Offset(detectLeft + detectWidth, detectTop + detectHeight)

                ).forEach { point ->

                    val left = point.x == detectLeft

                    val top = point.y == detectTop

                    val x = if (left) point.x + corner else point.x - corner

                    val y = if (top) point.y + corner else point.y - corner

                    val hEnd = if (left) x + line else x - line

                    val vEnd = if (top) y + line else y - line



                    drawLine(

                        color = Color(0xFF3B82F6).copy(alpha = borderAlpha),

                        start = Offset(x, y),

                        end = Offset(hEnd, y),

                        strokeWidth = stroke,

                        cap = StrokeCap.Round

                    )

                    drawLine(

                        color = Color(0xFF3B82F6).copy(alpha = borderAlpha),

                        start = Offset(x, y),

                        end = Offset(x, vEnd),

                        strokeWidth = stroke,

                        cap = StrokeCap.Round

                    )

                }

            }



            // Status message

            Column(

                modifier = Modifier

                    .align(Alignment.BottomCenter)

                    .padding(bottom = 16.dp),

                horizontalAlignment = Alignment.CenterHorizontally

            ) {

                Surface(

                    shape = RoundedCornerShape(12.dp),

                    color = Color.Black.copy(alpha = 0.6f),

                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))

                ) {

                    Row(

                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),

                        verticalAlignment = Alignment.CenterVertically

                    ) {

                        Box(

                            modifier = Modifier

                                .size(8.dp)

                                .background(Color(0xFF22C55E), CircleShape)

                        )

                        Spacer(Modifier.width(8.dp))

                        Text(

                            "Otomatik Kenar Tespiti Aktif",

                            style = MaterialTheme.typography.labelSmall,

                            color = Color.White,

                            fontWeight = FontWeight.Medium

                        )

                    }

                }

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
    val addingToNote = state.pendingScanNoteId != null

    if (addingToNote) {
        PendingBoardCropScreen(
            state = state,
            viewModel = viewModel,
            onRetry = { viewModel.detectBackendBoardForPendingNote() },
            onContinue = onContinue
        )
        return
    }

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

private fun PendingBoardCropScreen(

    state: Board2NotesUiState,

    viewModel: Board2NotesViewModel,

    onRetry: () -> Unit,

    onContinue: () -> Unit

) {

    val detection = state.boardDetection

    Column(

        modifier = Modifier

            .fillMaxSize()

            .background(darkAwareColor(Color.White, MaterialTheme.colorScheme.background))

    ) {

        Column(

            modifier = Modifier

                .weight(1f)

                .padding(horizontal = 18.dp)

        ) {

            Spacer(Modifier.height(14.dp))

            Text(

                "Tahta köşelerini kontrol et",

                style = MaterialTheme.typography.titleLarge,

                fontWeight = FontWeight.SemiBold,

                color = MaterialTheme.colorScheme.onSurface

            )

            Spacer(Modifier.height(6.dp))

            Text(

                "Köşe noktalarını gerekiyorsa sürükle. Sonraki adımda çıktı doğrudan notuna eklenecek.",

                style = MaterialTheme.typography.bodyMedium,

                color = MaterialTheme.colorScheme.onSurfaceVariant

            )

            Spacer(Modifier.height(16.dp))

            detection?.warnings?.filterNot(::isManualCropAppliedWarning)?.firstOrNull()?.let {

                WarningText(it)

                Spacer(Modifier.height(10.dp))

            }

            Surface(

                modifier = Modifier

                    .fillMaxWidth()

                    .weight(1f),

                shape = RoundedCornerShape(22.dp),

                color = darkAwareColor(Color(0xFFF8FAFC), MaterialTheme.colorScheme.surfaceVariant),

                border = BorderStroke(1.dp, darkAwareColor(Color(0xFFE2E8F0), MaterialTheme.colorScheme.outlineVariant))

            ) {

                Box(

                    modifier = Modifier

                        .fillMaxSize()

                        .padding(10.dp),

                    contentAlignment = Alignment.Center

                ) {

                    if (detection != null) {

                        ImageWithQuad(

                            bitmap = detection.overlayBitmap,

                            quad = state.manualQuad,

                            onCornerDrag = viewModel::updateManualCorner,

                            modifier = Modifier.fillMaxWidth()

                        )

                    } else {

                        StatusBanner("Köşeler hazırlanıyor...", busy = true)

                    }

                }

            }

            Spacer(Modifier.height(12.dp))

            Text(

                "İpucu: Mavi noktaları yalnızca tahta yüzeyinin gerçek köşelerine taşı.",

                style = MaterialTheme.typography.bodySmall,

                color = MaterialTheme.colorScheme.onSurfaceVariant

            )

            Spacer(Modifier.height(12.dp))

        }

        Surface(

            modifier = Modifier.fillMaxWidth(),

            color = MaterialTheme.colorScheme.surface,

            shadowElevation = 8.dp,

            tonalElevation = 0.dp

        ) {

            Row(

                horizontalArrangement = Arrangement.spacedBy(12.dp),

                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)

            ) {

                OutlinedButton(

                    onClick = onRetry,

                    modifier = Modifier

                        .weight(1f)

                        .height(50.dp),

                    enabled = !state.isBusy && state.selectedImage != null,

                    shape = RoundedCornerShape(16.dp)

                ) {

                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))

                    Spacer(Modifier.width(8.dp))

                    Text("Tekrar bul")

                }

                Button(

                    onClick = onContinue,

                    modifier = Modifier

                        .weight(1.25f)

                        .height(50.dp),

                    enabled = !state.isBusy && detection != null && state.manualQuad != null,

                    shape = RoundedCornerShape(16.dp)

                ) {

                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))

                    Spacer(Modifier.width(8.dp))

                    Text("Sonraki adıma geç", fontWeight = FontWeight.SemiBold)

                }

            }

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

        border = BorderStroke(1.dp, darkAwareColor(Color(0xFFE2E8F0), MaterialTheme.colorScheme.outlineVariant))

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

                    color = darkAwareColor(Color(0xFFEFF6FF), MaterialTheme.colorScheme.primaryContainer)

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

                color = darkAwareColor(Color(0xFFF8FAFC), MaterialTheme.colorScheme.surfaceVariant),

                border = BorderStroke(1.dp, darkAwareColor(Color(0xFFE2E8F0), MaterialTheme.colorScheme.outlineVariant))

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

        border = BorderStroke(1.dp, darkAwareColor(Color(0xFFE2E8F0), MaterialTheme.colorScheme.outlineVariant))

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

        border = BorderStroke(1.dp, darkAwareColor(Color(0xFFE2E8F0), MaterialTheme.colorScheme.outlineVariant))

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

        border = BorderStroke(1.dp, darkAwareColor(Color(0xFFE2E8F0), MaterialTheme.colorScheme.outlineVariant))

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

        border = BorderStroke(1.dp, darkAwareColor(Color(0xFFE2E8F0), MaterialTheme.colorScheme.outlineVariant))

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

                    color = darkAwareColor(Color(0xFFF8FAFC), MaterialTheme.colorScheme.surfaceVariant),

                    border = BorderStroke(1.dp, darkAwareColor(Color(0xFFE2E8F0), MaterialTheme.colorScheme.outlineVariant))

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



@Composable

private fun SamsungStyleNoteEditorScreen(

    state: Board2NotesUiState,

    viewModel: Board2NotesViewModel,

    onScan: (Int) -> Unit,

    onNotes: () -> Unit

) {

    val note = state.note ?: return

    val savedNote = state.selectedSavedNote

    val noteType = savedNote?.noteType ?: NoteType.Text

    val context = LocalContext.current

    var title by remember(note.createdAtEpochMs, state.activeSavedNoteId) { mutableStateOf(note.title) }

    var course by remember(note.createdAtEpochMs, state.activeSavedNoteId) { mutableStateOf(note.courseName) }



    // Yazı notu içeriği (Text türü)

    val initialDoc = remember(note.createdAtEpochMs, state.activeSavedNoteId) { CanvasDocument.fromBody(note.body) }
    val allowLegacyBackgroundFallback = remember(note.createdAtEpochMs, state.activeSavedNoteId) {
        !CanvasDocumentCodec.hasStructuredDocument(note.body)
    }

    var cleanText by remember(note.createdAtEpochMs, state.activeSavedNoteId) { mutableStateOf(initialDoc.text) }

    var body by remember(note.createdAtEpochMs, state.activeSavedNoteId) { mutableStateOf(note.body) }

    // Yazı notu: blok modeli (metin + görsel)
    val textBlocks = remember(note.createdAtEpochMs, state.activeSavedNoteId) {
        androidx.compose.runtime.mutableStateListOf<TextNoteBlock>().also {
            it.addAll(TextNoteCodec.parse(note.body).ifEmpty { listOf(TextNoteBlock.Text("")) })
        }
    }
    val textBlockTfvs = remember(note.createdAtEpochMs, state.activeSavedNoteId) {
        androidx.compose.runtime.mutableStateMapOf<String, TextFieldValue>()
    }
    val textBlockIds = remember(note.createdAtEpochMs, state.activeSavedNoteId) {
        androidx.compose.runtime.mutableStateListOf<String>().also {
            repeat(textBlocks.size) { _ -> it.add(java.util.UUID.randomUUID().toString()) }
        }
    }
    val textBlockFocusRequesters = remember(note.createdAtEpochMs, state.activeSavedNoteId) {
        androidx.compose.runtime.mutableStateMapOf<String, FocusRequester>()
    }
    var focusedTextBlockId by remember(note.createdAtEpochMs, state.activeSavedNoteId) {
        mutableStateOf<String?>(
            textBlockIds.indices.firstOrNull { textBlocks[it] is TextNoteBlock.Text }?.let { textBlockIds[it] }
        )
    }
    var pendingFocusBlockId by remember(note.createdAtEpochMs, state.activeSavedNoteId) {
        mutableStateOf<String?>(null)
    }

    fun syncTextNoteBody() {
        body = TextNoteCodec.serialize(textBlocks.toList())
        cleanText = TextNoteCodec.stripMarker(body)
    }



    // Canvas durumu (Canvas türü) — denetleyici tüm çizim durumunu tutar

    var canvasBody by remember(note.createdAtEpochMs, state.activeSavedNoteId) { mutableStateOf(note.body) }

    val controller = remember(note.createdAtEpochMs, state.activeSavedNoteId) {

        CanvasController(initialDoc.pages) { pages ->

            canvasBody = CanvasDocumentCodec.serialize(initialDoc.text, pages)

        }

    }

    LaunchedEffect(state.settings.defaultPenWidth, state.settings.defaultEraserSize) {

        controller.strokeWidth = state.settings.defaultPenWidth

        controller.eraserSize = state.settings.defaultEraserSize

    }

    var pendingInsert by remember(note.createdAtEpochMs, state.activeSavedNoteId) { mutableStateOf<String?>(null) }
    var pendingInsertPageIndex by remember(note.createdAtEpochMs, state.activeSavedNoteId) { mutableStateOf<Int?>(null) }
    var convertingAssets by remember(state.activeSavedNoteId) { mutableStateOf(emptySet<String>()) }

    var recentColors by remember { mutableStateOf(listOf(Color.Black, Color(0xFF2563EB), Color(0xFFEF4444), Color(0xFF10B981))) }



    var showAddMenu by remember { mutableStateOf(false) }

    var showMoreMenu by remember { mutableStateOf(false) }

    var courseDropdownExpanded by remember { mutableStateOf(false) }

    var showNewCourseDialog by remember { mutableStateOf(false) }

    var newCourseName by remember { mutableStateOf("") }

    var saveStatus by remember(state.activeSavedNoteId) { mutableStateOf("Kaydedildi") }

    var canvasChromeVisible by remember(state.activeSavedNoteId) { mutableStateOf(true) }

    var canvasFullscreen by remember(state.activeSavedNoteId) { mutableStateOf(false) }

    var aiState by remember(state.activeSavedNoteId) { mutableStateOf<NoteAiState>(NoteAiState.Idle) }

    var aiPopupOpen by remember(state.activeSavedNoteId) { mutableStateOf(false) }

    var aiLastExplainedText by remember(state.activeSavedNoteId) { mutableStateOf<String?>(null) }



    val scope = rememberCoroutineScope()

    val galleryInsertLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->

        val id = state.activeSavedNoteId

        if (uri != null && id != null) {

            scope.launch {

                val bmp = withContext(Dispatchers.IO) { runCatching { BitmapLoader.decodeFromUri(context, uri) }.getOrNull() }

                if (bmp != null) {

                    val asset = CanvasAssets.newImageAssetName()

                    withContext(Dispatchers.IO) {

                        CanvasAssets.imageFile(context.filesDir, id, asset).outputStream().use { out ->

                            bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)

                        }

                    }

                    pendingInsertPageIndex = controller.safePageIndex
                    pendingInsert = asset

                }

            }

        }

    }

    val textGalleryInsertLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val id = state.activeSavedNoteId
        if (uri != null && id != null) {
            scope.launch {
                val bmp = withContext(Dispatchers.IO) {
                    runCatching { BitmapLoader.decodeFromUri(context, uri) }.getOrNull()
                }
                if (bmp != null) {
                    val asset = CanvasAssets.newImageAssetName()
                    withContext(Dispatchers.IO) {
                        val file = CanvasAssets.imageFile(context.filesDir, id, asset)
                        file.parentFile?.mkdirs()
                        file.outputStream().use { out ->
                            bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                        }
                    }
                    val focusedId = focusedTextBlockId
                    val fi = focusedId?.let { textBlockIds.indexOf(it) } ?: -1
                    val insertionIndex = if (fi >= 0 && fi < textBlocks.size && textBlocks[fi] is TextNoteBlock.Text) fi else textBlocks.lastIndex
                    val current = (textBlocks.getOrNull(insertionIndex) as? TextNoteBlock.Text)?.text.orEmpty()
                    val caret = focusedId?.let { textBlockTfvs[it]?.selection?.start }?.coerceIn(0, current.length) ?: current.length
                    val pre = current.substring(0, caret)
                    val post = current.substring(caret)
                    val imageBlockId = java.util.UUID.randomUUID().toString()
                    val postId = java.util.UUID.randomUUID().toString()
                    if (insertionIndex >= 0) {
                        textBlocks[insertionIndex] = TextNoteBlock.Text(pre)
                        textBlockTfvs[textBlockIds[insertionIndex]] = TextFieldValue(pre, TextRange(pre.length))
                        textBlocks.add(insertionIndex + 1, TextNoteBlock.Image(asset))
                        textBlockIds.add(insertionIndex + 1, imageBlockId)
                        textBlocks.add(insertionIndex + 2, TextNoteBlock.Text(post))
                        textBlockIds.add(insertionIndex + 2, postId)
                        textBlockTfvs[postId] = TextFieldValue(post, TextRange(0))
                    } else {
                        textBlocks.add(TextNoteBlock.Image(asset))
                        textBlockIds.add(imageBlockId)
                        textBlocks.add(TextNoteBlock.Text(""))
                        textBlockIds.add(postId)
                        textBlockTfvs[postId] = TextFieldValue("", TextRange(0))
                    }
                    focusedTextBlockId = postId
                    pendingFocusBlockId = postId
                    syncTextNoteBody()
                }
            }
        }
    }

    LaunchedEffect(state.pendingCanvasImageAsset, state.pendingCanvasImagePageIndex) {

        val pending = state.pendingCanvasImageAsset

        if (pending != null) {
            pendingInsert = pending
            pendingInsertPageIndex = state.pendingCanvasImagePageIndex
            viewModel.clearPendingCanvasImage()
        }

    }



    val courseColorSnapshot = customCourseColors.toMap()

    val existingCourses = remember(state.savedNotes, courseColorSnapshot) {

        (state.savedNotes.map { it.courseName } + courseColorSnapshot.keys)
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()

    }

    val wordCount = remember(cleanText) {

        cleanText.split("\\s+".toRegex()).count { it.isNotBlank() }

    }



    LaunchedEffect(title, course, body, canvasBody, noteType) {

        saveStatus = "Kaydediliyor"

        delay(700)

        val effectiveBody = if (noteType == NoteType.Canvas) canvasBody else body

        viewModel.updateNote(title, effectiveBody, course)

        viewModel.autoSaveCurrentNote()

        saveStatus = "Kaydedildi"

    }

    var exitRequested by remember(state.activeSavedNoteId) { mutableStateOf(false) }

    fun saveAndExit() {
        if (exitRequested) return
        exitRequested = true
        val effectiveBody = if (noteType == NoteType.Canvas) canvasBody else body
        viewModel.updateNote(title, effectiveBody, course)
        viewModel.saveCurrentNote(
            onSaved = onNotes,
            onFailed = { exitRequested = false }
        )
    }

    fun runAiExplanation(combinedText: String) {
        aiState = NoteAiState.Loading
        aiPopupOpen = true
        scope.launch {
            runCatching { viewModel.explainCurrentNote(combinedText, title) }
                .onSuccess {
                    aiState = NoteAiState.Success(it)
                    aiLastExplainedText = combinedText
                }
                .onFailure {
                    aiState = NoteAiState.Error(it.message ?: "Açıklama alınamadı.")
                }
        }
    }

    BackHandler(onBack = ::saveAndExit)

    Box(modifier = Modifier.fillMaxSize()) {
        Column(

            modifier = Modifier

                .fillMaxSize()

                .background(MaterialTheme.colorScheme.background)

        ) {

        AnimatedVisibility(
            visible = noteType != NoteType.Canvas || (!canvasFullscreen && canvasChromeVisible),
            enter = fadeIn(tween(140)) + slideInVertically(tween(180)) { -it },
            exit = fadeOut(tween(110)) + slideOutVertically(tween(150)) { -it }
        ) {

        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp, shadowElevation = 0.dp) {

            Row(

                modifier = Modifier

                    .fillMaxWidth()

                    .height(56.dp)

                    .padding(horizontal = 4.dp),

                verticalAlignment = Alignment.CenterVertically

            ) {

                IconButton(onClick = ::saveAndExit) {

                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")

                }

                androidx.compose.foundation.text.BasicTextField(

                    value = title,

                    onValueChange = { title = it.ifBlank { "" } },

                    textStyle = MaterialTheme.typography.titleMedium.copy(

                        fontSize = 20.sp,

                        color = MaterialTheme.colorScheme.onSurface,

                        fontWeight = FontWeight.SemiBold

                    ),

                    singleLine = true,

                    modifier = Modifier

                        .weight(1f)

                        .padding(end = 4.dp),

                    decorationBox = { inner ->

                        if (title.isBlank()) {

                            Text(

                                "Başlık",

                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp),

                                fontWeight = FontWeight.SemiBold,

                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)

                            )

                        }

                        inner()

                    }

                )



                Box {

                    IconButton(onClick = { showAddMenu = true }) {

                        Icon(Icons.Default.Add, contentDescription = "Ekle")

                    }

                    DropdownMenu(

                        expanded = showAddMenu,

                        onDismissRequest = { showAddMenu = false },

                        modifier = Modifier

                            .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp))

                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))

                            .width(200.dp),

                        shape = RoundedCornerShape(16.dp)

                    ) {

                        val itemColor = MaterialTheme.colorScheme.onSurface



                        DropdownMenuItem(

                            text = { Text("Tahtadan ekle", fontWeight = FontWeight.Medium, color = itemColor) },

                            leadingIcon = { Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary) },

                            onClick = {

                                showAddMenu = false

                                viewModel.updateNote(title, if (noteType == NoteType.Canvas) canvasBody else body, course)

                                viewModel.autoSaveCurrentNote()

                                onScan(controller.safePageIndex)

                            }

                        )

                        DropdownMenuItem(

                            text = { Text("Görsel ekle", fontWeight = FontWeight.Medium, color = itemColor) },

                            leadingIcon = { Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.secondary) },

                            onClick = {

                                showAddMenu = false

                                if (noteType == NoteType.Canvas) {
                                    galleryInsertLauncher.launch("image/*")
                                } else {
                                    textGalleryInsertLauncher.launch("image/*")
                                }

                            }

                        )

                    }

                }

                Box {

                    IconButton(onClick = { showMoreMenu = true }) {

                        Icon(Icons.Default.MoreVert, contentDescription = "Daha fazla")

                    }

                    DropdownMenu(

                        expanded = showMoreMenu,

                        onDismissRequest = { showMoreMenu = false },

                        modifier = Modifier

                            .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp))

                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))

                            .width(230.dp),

                        shape = RoundedCornerShape(16.dp)

                    ) {

                        val itemColor = MaterialTheme.colorScheme.onSurface



                        DropdownMenuItem(

                            text = { Text("Ders seç", fontWeight = FontWeight.Medium, color = itemColor) },

                            leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary) },

                            onClick = {

                                showMoreMenu = false

                                courseDropdownExpanded = true

                            }

                        )



                        DropdownMenuItem(

                            text = { Text("Kaydet ve notlara dön", fontWeight = FontWeight.Medium, color = itemColor) },

                            leadingIcon = { Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary) },

                            onClick = {

                                showMoreMenu = false

                                saveAndExit()

                            }

                        )

                        DropdownMenuItem(

                            text = { Text("Kopyala", fontWeight = FontWeight.Medium, color = itemColor) },

                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.secondary) },

                            onClick = {

                                showMoreMenu = false

                                copyNote(context, state.note ?: note)

                            }

                        )

                        DropdownMenuItem(

                            text = { Text("Paylaş", fontWeight = FontWeight.Medium, color = itemColor) },

                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.secondary) },

                            onClick = {

                                showMoreMenu = false

                                shareNote(context, state.note ?: note)

                            }

                        )

                    }

                }

            }

        }

        }


        if (noteType == NoteType.Text) {

            val noteId = state.activeSavedNoteId
            val emptyDocAndPlaceholderShown = textBlocks.size == 1 &&
                (textBlocks[0] as? TextNoteBlock.Text)?.text.isNullOrEmpty()

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp, vertical = 8.dp)
            ) {
                textBlocks.forEachIndexed { index, block ->
                    val id = textBlockIds.getOrNull(index) ?: return@forEachIndexed
                    when (block) {
                        is TextNoteBlock.Text -> {
                            val tfv = textBlockTfvs.getOrPut(id) {
                                TextFieldValue(block.text, TextRange(block.text.length))
                            }
                            val focusRequester = textBlockFocusRequesters.getOrPut(id) { FocusRequester() }
                            if (pendingFocusBlockId == id) {
                                LaunchedEffect(id) {
                                    runCatching { focusRequester.requestFocus() }
                                    pendingFocusBlockId = null
                                }
                            }
                            androidx.compose.foundation.text.BasicTextField(
                                value = tfv,
                                onValueChange = { newTfv ->
                                    textBlockTfvs[id] = newTfv
                                    val currentIdx = textBlockIds.indexOf(id)
                                    if (currentIdx >= 0 && currentIdx < textBlocks.size) {
                                        textBlocks[currentIdx] = TextNoteBlock.Text(newTfv.text)
                                    }
                                    syncTextNoteBody()
                                },
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 17.sp,
                                    lineHeight = 27.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester)
                                    .onFocusChanged { if (it.isFocused) focusedTextBlockId = id },
                                decorationBox = { inner ->
                                    if (emptyDocAndPlaceholderShown && index == 0) {
                                        Text(
                                            "Not yazmaya başlayın...",
                                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                                        )
                                    }
                                    inner()
                                }
                            )
                        }
                        is TextNoteBlock.Image -> {
                            val bmp = remember(block.asset, noteId) {
                                if (noteId.isNullOrEmpty()) null
                                else runCatching {
                                    BitmapFactory.decodeFile(
                                        CanvasAssets.imageFile(context.filesDir, noteId, block.asset).absolutePath
                                    )
                                }.getOrNull()
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                if (bmp != null) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "Eklenen görsel",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        val currentIdx = textBlockIds.indexOf(id)
                                        if (currentIdx < 0) return@IconButton
                                        val prev = textBlocks.getOrNull(currentIdx - 1) as? TextNoteBlock.Text
                                        val next = textBlocks.getOrNull(currentIdx + 1) as? TextNoteBlock.Text
                                        if (prev != null && next != null) {
                                            val prevId = textBlockIds[currentIdx - 1]
                                            val mergedText = prev.text + next.text
                                            textBlocks[currentIdx - 1] = TextNoteBlock.Text(mergedText)
                                            textBlockTfvs[prevId] = TextFieldValue(mergedText, TextRange(prev.text.length))
                                            textBlocks.removeAt(currentIdx + 1)
                                            textBlockIds.removeAt(currentIdx + 1)
                                            textBlocks.removeAt(currentIdx)
                                            textBlockIds.removeAt(currentIdx)
                                            focusedTextBlockId = prevId
                                            pendingFocusBlockId = prevId
                                        } else {
                                            textBlocks.removeAt(currentIdx)
                                            textBlockIds.removeAt(currentIdx)
                                        }
                                        noteId?.let { nid ->
                                            runCatching {
                                                CanvasAssets.imageFile(context.filesDir, nid, block.asset).delete()
                                            }
                                        }
                                        syncTextNoteBody()
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .size(28.dp)
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), CircleShape)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Görseli sil",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

        } else {

            CanvasEditor(

                controller = controller,

                filesDir = context.filesDir,

                noteId = state.activeSavedNoteId.orEmpty(),

                input = CanvasInputSettings(

                    allowFingerDrawing = state.settings.allowFingerDrawing,

                    useStylusPressure = state.settings.useStylusPressure,

                    palmRejection = state.settings.palmRejection,

                    minZoom = state.settings.canvasMinZoom,

                    maxZoom = state.settings.canvasMaxZoom

                ),

                recentColors = recentColors,

                onColorUsed = { picked -> recentColors = (listOf(picked) + recentColors).distinct().take(8) },

                onAddImage = { galleryInsertLauncher.launch("image/*") },

                pageControlsVisible = !canvasFullscreen && canvasChromeVisible,

                onChromeVisibleChange = { visible ->
                    if (!canvasFullscreen || !visible) canvasChromeVisible = visible
                },

                isFullscreen = canvasFullscreen,

                onFullscreenChange = { enabled ->
                    canvasFullscreen = enabled
                    canvasChromeVisible = !enabled
                },

                pendingInsertAsset = pendingInsert,

                pendingInsertPageIndex = pendingInsertPageIndex,

                pendingInsertJobId = state.pendingCanvasImageJobId,

                onPendingInsertConsumed = {
                    pendingInsert = null
                    pendingInsertPageIndex = null
                },

                convertingAssets = convertingAssets,

                onConvertImageToText = { imageElement ->
                    val noteId = state.activeSavedNoteId.orEmpty()
                    if (noteId.isNotBlank()) {
                        val file = CanvasAssets.imageFile(context.filesDir, noteId, imageElement.asset)
                        convertingAssets = convertingAssets + imageElement.asset
                        scope.launch {
                            runCatching {
                                val bmp = withContext(Dispatchers.IO) {
                                    if (file.exists()) android.graphics.BitmapFactory.decodeFile(file.absolutePath) else null
                                }
                                if (bmp != null) {
                                    val text = viewModel.ocrImageToText(bmp, imageElement.backendJobId)
                                    controller.setImageOcrText(imageElement.id, text)
                                } else {
                                    throw IllegalStateException("Görsel dosyası yüklenemedi.")
                                }
                            }.onFailure { err ->
                                android.widget.Toast.makeText(
                                    context,
                                    "Metne çevirme hatası: ${err.message}",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                            convertingAssets = convertingAssets - imageElement.asset
                        }
                    }
                },

                allowLegacyBackgroundFallback = allowLegacyBackgroundFallback,

                useDarkTheme = state.settings.useDarkTheme,

                modifier = Modifier

                    .weight(1f)

                    .fillMaxWidth()

            )

        }

    }

        if (noteType == NoteType.Canvas) {
            NoteAiAssistant(
                state = aiState,
                popupOpen = aiPopupOpen,
                onTogglePopup = {
                    if (aiState is NoteAiState.Loading) {
                        aiPopupOpen = !aiPopupOpen
                    } else {
                        val combined = controller.imagesOcrCombined()
                        if (combined.isBlank()) {
                            aiState = NoteAiState.Error("Bu notta metne çevrilmiş görsel yok. Önce görselleri 'Metne çevir' ile dönüştürün.")
                            aiPopupOpen = true
                        } else if (combined == aiLastExplainedText && (aiState is NoteAiState.Success || aiState is NoteAiState.Error)) {
                            aiPopupOpen = !aiPopupOpen
                        } else {
                            runAiExplanation(combined)
                        }
                    }
                },
                onDismiss = { aiPopupOpen = false },
                onRegenerate = {
                    val combined = controller.imagesOcrCombined()
                    if (combined.isNotBlank()) {
                        runAiExplanation(combined)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }



    if (courseDropdownExpanded) {

        CourseSelectorDialog(

            currentCourse = course,

            existingCourses = existingCourses,

            onSelect = { selectedCourseName ->

                course = selectedCourseName

                viewModel.updateNote(title, if (noteType == NoteType.Canvas) canvasBody else body, selectedCourseName)

                courseDropdownExpanded = false

            },

            onAddNewCourse = {

                showNewCourseDialog = true

                courseDropdownExpanded = false

            },

            onDismiss = { courseDropdownExpanded = false }

        )

    }



    if (showNewCourseDialog) {

        AlertDialog(

            onDismissRequest = { showNewCourseDialog = false },

            title = { Text("Yeni ders") },

            text = {

                OutlinedTextField(

                    value = newCourseName,

                    onValueChange = { newCourseName = it },

                    label = { Text("Ders adı") },

                    singleLine = true,

                    modifier = Modifier.fillMaxWidth()

                )

            },

            confirmButton = {

                Button(

                    onClick = {

                        if (newCourseName.isNotBlank()) {

                            course = newCourseName.trim()

                            viewModel.updateNote(title, if (noteType == NoteType.Canvas) canvasBody else body, course)

                        }

                        showNewCourseDialog = false

                        newCourseName = ""

                    }

                ) {

                    Text("Ekle")

                }

            },

            dismissButton = {

                TextButton(onClick = { showNewCourseDialog = false }) {

                    Text("Vazgeç")

                }

            }

        )

    }

}



@Composable

private fun NotesScreen(

    state: Board2NotesUiState,

    onOpen: (SavedNote) -> Unit,

    onFolderOpen: (String) -> Unit,

    onArchiveOpen: () -> Unit

) {

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current

    val screenWidthDp = configuration.screenWidthDp

    val columns = if (screenWidthDp >= 600) 4 else 2



    var query by remember { mutableStateOf("") }

    val courseColorSnapshot = customCourseColors.toMap()

    val groups = remember(state.savedNotes, courseColorSnapshot) {

        val noteGroups = state.savedNotes
            .filter { !it.isDeleted }

            .groupBy { it.courseName.ifBlank { "Genel" } }

        val allCourses = (noteGroups.keys + courseColorSnapshot.keys + "Genel")
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()

        allCourses.map { course ->

            course to (noteGroups[course] ?: emptyList())

        }

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

            latestEpochMs = latestEpochMs,

            archivedCount = state.archivedNotes.size,

            onArchiveOpen = onArchiveOpen

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

            if (state.savedNotes.isEmpty() && customCourseColors.isEmpty()) {

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

                val courseChunks = remember(groups, columns) { groups.chunked(columns) }

                Column(modifier = Modifier.padding(horizontal = 16.dp)) {

                    courseChunks.forEach { row ->

                        Row(

                            modifier = Modifier.fillMaxWidth(),

                            horizontalArrangement = Arrangement.spacedBy(14.dp)

                        ) {

                            row.forEach { (course, notes) ->

                                CourseFolderCard(

                                    courseName = course,

                                    noteCount = notes.size,

                                    thumbnailPath = notes.firstNotNullOfOrNull { it.previewImagePath() },

                                    onClick = { onFolderOpen(course) },

                                    modifier = Modifier.weight(1f)

                                )

                            }

                            val emptySlots = columns - row.size

                            repeat(emptySlots) {

                                Spacer(modifier = Modifier.weight(1f))

                            }

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

private fun NotesHeroHeader(

    totalCount: Int,

    latestEpochMs: Long?,

    archivedCount: Int,

    onArchiveOpen: () -> Unit

) {

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

                    Spacer(Modifier.width(10.dp))

                    CompactPillButton(

                        text = "Arşiv ($archivedCount)",

                        icon = Icons.Default.Folder,

                        onClick = onArchiveOpen

                    )

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

            FileThumbnail(note.previewImagePath())

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

            text = { Text("Bu not çöp kutusuna taşınacak. Daha sonra geri yükleyebilirsiniz.") },

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



        // Header Info Card (Modern Glass / Unified Look)

        Surface(

            modifier = Modifier.fillMaxWidth(),

            shape = RoundedCornerShape(28.dp),

            color = MaterialTheme.colorScheme.surface,

            tonalElevation = 1.dp,

            border = BorderStroke(1.dp, darkAwareColor(Color(0xFFE2E8F0), MaterialTheme.colorScheme.outlineVariant))

        ) {

            Column(Modifier.padding(20.dp)) {

                Row(verticalAlignment = Alignment.CenterVertically) {

                    Surface(

                        modifier = Modifier.size(52.dp),

                        shape = RoundedCornerShape(18.dp),

                        color = MaterialTheme.colorScheme.primaryContainer

                    ) {

                        Box(contentAlignment = Alignment.Center) {

                            Icon(

                                Icons.Default.Description,

                                contentDescription = null,

                                tint = MaterialTheme.colorScheme.primary,

                                modifier = Modifier.size(26.dp)

                            )

                        }

                    }

                    Spacer(Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {

                        Text(

                            note.title.ifBlank { "Başlıksız Not" },

                            style = MaterialTheme.typography.titleLarge,

                            fontWeight = FontWeight.Bold,

                            color = MaterialTheme.colorScheme.onSurface

                        )

                        Spacer(Modifier.height(6.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {

                            Surface(

                                shape = RoundedCornerShape(8.dp),

                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)

                            ) {

                                Text(

                                    note.courseName.ifBlank { "Genel" },

                                    style = MaterialTheme.typography.labelSmall,

                                    fontWeight = FontWeight.Bold,

                                    color = MaterialTheme.colorScheme.primary,

                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)

                                )

                            }

                            Spacer(Modifier.width(12.dp))

                            Text(

                                formatDate(note.updatedAtEpochMs),

                                style = MaterialTheme.typography.bodySmall,

                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)

                            )

                        }

                    }

                }

            }

        }



        Spacer(Modifier.height(16.dp))



        // Image View (Processed Whiteboard Image)

        val previewPath = note.previewImagePath()

        val bitmap = remember(previewPath) { previewPath?.let { BitmapFactory.decodeFile(it) } }

        if (bitmap != null) {

            Surface(

                modifier = Modifier.fillMaxWidth(),

                shape = RoundedCornerShape(24.dp),

                color = MaterialTheme.colorScheme.surface,

                border = BorderStroke(1.dp, darkAwareColor(Color(0xFFE2E8F0), MaterialTheme.colorScheme.outlineVariant)),

                tonalElevation = 1.dp

            ) {

                Column(Modifier.padding(12.dp)) {

                    Text(

                        "İşlenmiş Tahta Görseli",

                        style = MaterialTheme.typography.titleSmall,

                        fontWeight = FontWeight.Bold,

                        color = MaterialTheme.colorScheme.onSurfaceVariant,

                        modifier = Modifier.padding(start = 6.dp, bottom = 8.dp, top = 4.dp)

                    )

                    Box(

                        modifier = Modifier

                            .fillMaxWidth()

                            .heightIn(max = 240.dp)

                            .clip(RoundedCornerShape(16.dp))

                    ) {

                        PreviewImage(bitmap)

                    }

                }

            }

            Spacer(Modifier.height(16.dp))

        }



        // Note Body Content Card

        Surface(

            modifier = Modifier.fillMaxWidth(),

            shape = RoundedCornerShape(24.dp),

            color = MaterialTheme.colorScheme.surface,

            tonalElevation = 1.dp,

            border = BorderStroke(1.dp, darkAwareColor(Color(0xFFE2E8F0), MaterialTheme.colorScheme.outlineVariant))

        ) {

            Column(Modifier.padding(20.dp)) {

                Row(verticalAlignment = Alignment.CenterVertically) {

                    Icon(

                        Icons.Default.TextFields,

                        contentDescription = null,

                        tint = MaterialTheme.colorScheme.primary,

                        modifier = Modifier.size(20.dp)

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

                

                // Rich Text View (Clean formatting)

                val noteBody = note.body

                val isCanvas = note.noteType == NoteType.Canvas ||

                        noteBody.contains("[[B2N_CANVAS_V2]]") ||

                        noteBody.contains("[CanvasPages:") ||

                        noteBody.contains("[DrawingData:")



                if (isCanvas) {

                    val cleanText = noteBody.substringBefore("[[B2N_CANVAS_V2]]")

                        .substringBefore("[CanvasPages:")

                        .substringBefore("[DrawingData:")

                        .trim()



                    Text(

                        cleanText.ifBlank { "Metin içeriği bulunmuyor, çizim sekmesinden düzenleyebilirsiniz." },

                        style = MaterialTheme.typography.bodyLarge,

                        color = MaterialTheme.colorScheme.onSurface,

                        lineHeight = 24.sp

                    )



                    Spacer(modifier = Modifier.height(12.dp))



                    Surface(

                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),

                        shape = RoundedCornerShape(12.dp),

                        modifier = Modifier.fillMaxWidth()

                    ) {



                        Row(

                            modifier = Modifier.padding(12.dp),

                            verticalAlignment = Alignment.CenterVertically

                        ) {



                            Icon(

                                imageVector = Icons.Default.Brush,

                                contentDescription = null,

                                tint = MaterialTheme.colorScheme.primary,

                                modifier = Modifier.size(18.dp)

                            )



                            Spacer(modifier = Modifier.width(8.dp))



                            Text(

                                "Bu not bir çizim içeriyor. Düzenle butonuna basarak çiziminizi görüntüleyebilirsiniz.",

                                style = MaterialTheme.typography.bodySmall,

                                color = MaterialTheme.colorScheme.primary

                            )



                        }



                    }



                } else {

                    val parsedBlocks = remember(noteBody) { TextNoteCodec.parse(noteBody) }
                    val isEmpty = parsedBlocks.isEmpty() || (parsedBlocks.size == 1 &&
                        (parsedBlocks[0] as? TextNoteBlock.Text)?.text.isNullOrBlank())
                    if (isEmpty) {
                        Text(
                            "Boş not",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 24.sp
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            parsedBlocks.forEach { block ->
                                when (block) {
                                    is TextNoteBlock.Text -> {
                                        if (block.text.isNotEmpty()) {
                                            Text(
                                                block.text,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                lineHeight = 24.sp
                                            )
                                        }
                                    }
                                    is TextNoteBlock.Image -> {
                                        val bmpImg = remember(block.asset, note.id) {
                                            runCatching {
                                                BitmapFactory.decodeFile(
                                                    CanvasAssets.imageFile(context.filesDir, note.id, block.asset).absolutePath
                                                )
                                            }.getOrNull()
                                        }
                                        if (bmpImg != null) {
                                            Image(
                                                bitmap = bmpImg.asImageBitmap(),
                                                contentDescription = "Görsel",
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(12.dp))
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                }

            }

        }



        Spacer(Modifier.height(16.dp))



        // Premium Modern Action Buttons Row (Unified single row instead of multiple blocks)

        Surface(

            modifier = Modifier.fillMaxWidth(),

            shape = RoundedCornerShape(24.dp),

            color = MaterialTheme.colorScheme.surface,

            tonalElevation = 1.dp,

            border = BorderStroke(1.dp, darkAwareColor(Color(0xFFE2E8F0), MaterialTheme.colorScheme.outlineVariant))

        ) {

            Row(

                modifier = Modifier.padding(12.dp),

                horizontalArrangement = Arrangement.spacedBy(10.dp)

            ) {

                // Edit (Translucent Blue)

                FilledTonalButton(

                    onClick = onEdit,

                    modifier = Modifier.weight(1f).height(48.dp),

                    shape = RoundedCornerShape(16.dp),

                    colors = ButtonDefaults.filledTonalButtonColors(

                        containerColor = Color(0xFFDBEAFE).copy(alpha = 0.7f),

                        contentColor = Color(0xFF1D4ED8)

                    )

                ) {

                    Icon(Icons.Default.Edit, contentDescription = "Düzenle", modifier = Modifier.size(20.dp))

                }



                // Copy (Translucent Slate/Gray)

                FilledTonalButton(

                    onClick = { copySavedNote(context, note) },

                    modifier = Modifier.weight(1f).height(48.dp),

                    shape = RoundedCornerShape(16.dp),

                    colors = ButtonDefaults.filledTonalButtonColors(

                        containerColor = Color(0xFFE2E8F0).copy(alpha = 0.7f),

                        contentColor = Color(0xFF475569)

                    )

                ) {

                    Icon(Icons.Default.ContentCopy, contentDescription = "Kopyala", modifier = Modifier.size(20.dp))

                }



                // Share (Translucent Green)

                FilledTonalButton(

                    onClick = { shareSavedNote(context, note) },

                    modifier = Modifier.weight(1f).height(48.dp),

                    shape = RoundedCornerShape(16.dp),

                    colors = ButtonDefaults.filledTonalButtonColors(

                        containerColor = Color(0xFFDCFCE7).copy(alpha = 0.7f),

                        contentColor = Color(0xFF15803D)

                    )

                ) {

                    Icon(Icons.Default.Share, contentDescription = "Paylaş", modifier = Modifier.size(20.dp))

                }



                // Delete (Translucent Red)

                FilledTonalButton(

                    onClick = { showDeleteDialog = true },

                    modifier = Modifier.weight(1f).height(48.dp),

                    shape = RoundedCornerShape(16.dp),

                    colors = ButtonDefaults.filledTonalButtonColors(

                        containerColor = Color(0xFFFEE2E2).copy(alpha = 0.7f),

                        contentColor = Color(0xFFB91C1C)

                    )

                ) {

                    Icon(Icons.Default.Delete, contentDescription = "Sil", modifier = Modifier.size(20.dp))

                }

            }

        }



        if (state.settings.debugMode) {

            Spacer(Modifier.height(16.dp))

            FilePreview(note.cropImagePath, "Crop")

            Spacer(Modifier.height(8.dp))

            FilePreview(note.ocrImagePath, "OCR görüntüsü")

            Spacer(Modifier.height(8.dp))

            Text("OCR ham metni:\n${note.ocrText}", style = MaterialTheme.typography.bodySmall)

        }

        Spacer(Modifier.height(120.dp))

    }

}



@Composable

private fun ArchiveScreen(

    state: Board2NotesUiState,

    viewModel: Board2NotesViewModel,

    onOpen: (SavedNote) -> Unit

) {

    var pendingDelete by remember { mutableStateOf<SavedNote?>(null) }

    pendingDelete?.let { note ->

        ConfirmDeleteNoteDialog(

            onDismiss = { pendingDelete = null },

            onConfirm = {

                pendingDelete = null

                viewModel.deleteNoteById(note.id)

            }

        )

    }

    ScreenColumn {

        Spacer(Modifier.height(18.dp))

        Surface(

            modifier = Modifier.fillMaxWidth(),

            shape = RoundedCornerShape(24.dp),

            color = MaterialTheme.colorScheme.surface,

            tonalElevation = 1.dp,

            border = BorderStroke(1.dp, darkAwareColor(Color(0xFFE2E8F0), MaterialTheme.colorScheme.outlineVariant))

        ) {

            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {

                Surface(

                    modifier = Modifier.size(48.dp),

                    shape = RoundedCornerShape(16.dp),

                    color = MaterialTheme.colorScheme.primaryContainer

                ) {

                    Box(contentAlignment = Alignment.Center) {

                        Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)

                    }

                }

                Spacer(Modifier.width(14.dp))

                Column {

                    Text("Arşiv", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

                    Text(

                        "${state.archivedNotes.size} not arşivde",

                        style = MaterialTheme.typography.bodyMedium,

                        color = MaterialTheme.colorScheme.onSurfaceVariant

                    )

                }

            }

        }

        Spacer(Modifier.height(16.dp))

        if (state.archivedNotes.isEmpty()) {

            EmptyState("Arşivde not yok.", "Arşivlenen notlar burada görünür.")

        } else {

            state.archivedNotes.forEach { note ->

                ArchiveNoteCard(

                    note = note,

                    onOpen = { onOpen(note) },

                    onRestore = { viewModel.unarchiveNote(note.id) },

                    onDelete = { pendingDelete = note }

                )

                Spacer(Modifier.height(12.dp))

            }

        }

        Spacer(Modifier.height(18.dp))

    }

}



@Composable

private fun ArchiveNoteCard(

    note: SavedNote,

    onOpen: () -> Unit,

    onRestore: () -> Unit,

    onDelete: () -> Unit

) {

    Surface(

        modifier = Modifier.fillMaxWidth(),

        shape = RoundedCornerShape(20.dp),

        color = MaterialTheme.colorScheme.surface,

        tonalElevation = 1.dp,

        border = BorderStroke(1.dp, darkAwareColor(Color(0xFFE2E8F0), MaterialTheme.colorScheme.outlineVariant))

    ) {

        Column(Modifier.padding(14.dp)) {

            Row(

                modifier = Modifier

                    .fillMaxWidth()

                    .clickable(onClick = onOpen),

                verticalAlignment = Alignment.CenterVertically

            ) {

                Column(modifier = Modifier.weight(1f)) {

                    Text(note.title.ifBlank { "Başlıksız not" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                    Spacer(Modifier.height(5.dp))

                    Text(formatDate(note.updatedAtEpochMs), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)

                    Spacer(Modifier.height(7.dp))

                    Text(note.preview.ifBlank { "Boş not" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)

                }

                Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)

            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {

                SecondaryActionButton("Geri Yükle", Icons.Default.Refresh, Modifier.weight(1f), onRestore)

                DangerOutlineButton("Sil", Icons.Default.Delete, Modifier.weight(1f), onDelete)

            }

        }

    }

}



@Composable

private fun SamsungSettingsScreen(

    state: Board2NotesUiState,

    viewModel: Board2NotesViewModel,

    onAbout: () -> Unit

) {

    var activeCategoryIndex by remember { mutableIntStateOf(0) }

    val categories = listOf("Genel & Görünüm", "Çizim & Canvas", "AI & Backend", "Sistem & Hakkında")



    ScreenColumn {

        Spacer(Modifier.height(10.dp))

        Text(

            "Ayarlar",

            style = MaterialTheme.typography.titleLarge,

            fontWeight = FontWeight.SemiBold,

            color = MaterialTheme.colorScheme.onSurface

        )

        Spacer(Modifier.height(14.dp))



        // Horizontal pill category selector

        Row(

            modifier = Modifier

                .fillMaxWidth()

                .horizontalScroll(rememberScrollState())

                .padding(vertical = 4.dp),

            horizontalArrangement = Arrangement.spacedBy(8.dp)

        ) {

            categories.forEachIndexed { index, name ->

                val isSelected = activeCategoryIndex == index

                Surface(

                    onClick = { activeCategoryIndex = index },

                    shape = RoundedCornerShape(20.dp),

                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),

                    border = if (isSelected) null else BorderStroke(1.dp, darkAwareColor(Color(0xFFE2E8F0), MaterialTheme.colorScheme.outlineVariant)),

                    modifier = Modifier.padding(bottom = 8.dp)

                ) {

                    Text(

                        text = name,

                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),

                        style = MaterialTheme.typography.labelLarge,

                        fontWeight = FontWeight.SemiBold,

                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

                    )

                }

            }

        }

        Spacer(Modifier.height(12.dp))



        when (activeCategoryIndex) {

            0 -> {

                // Genel & Görünüm

                SamsungSettingsSection("Görünüm") {

                    SettingSwitchRow(

                        Icons.Default.Palette,

                        "Koyu Tema",

                        "Uygulama ve Canvas arka planını koyu yapar",

                        state.settings.useDarkTheme,

                        viewModel::setUseDarkTheme

                    )

                    SettingInfoRow(Icons.Default.Language, "Dil", "Türkçe")

                    SettingSwitchRow(

                        Icons.Default.Menu,

                        "Alt gezinme çubuğu",

                        "Samsung Notes tarzı için varsayılan kapalı",

                        state.settings.showBottomNavigation,

                        viewModel::setShowBottomNavigation

                    )

                }

            }

            1 -> {

                // Çizim & Canvas

                SamsungSettingsSection("Canvas ve kalem") {

                    SettingSwitchRow(

                        Icons.Default.Brush,

                        "Parmakla çizim",

                        "Stylus yokken canvas çizimini açık tutar",

                        state.settings.allowFingerDrawing,

                        viewModel::setAllowFingerDrawing

                    )

                    SettingSwitchRow(

                        Icons.Default.Edit,

                        "Stylus basıncı",

                        "Destekli kalemlerde değişken kalınlık için hazır",

                        state.settings.useStylusPressure,

                        viewModel::setUseStylusPressure

                    )

                    SettingSwitchRow(

                        Icons.Default.Check,

                        "Avuç içi reddi",

                        "S Pen cihazlarda yanlış temasları azaltmak için hazırlık",

                        state.settings.palmRejection,

                        viewModel::setPalmRejection

                    )

                    Spacer(Modifier.height(10.dp))

                    Text(

                        "Kalem kalınlığı: ${"%.0f".format(state.settings.defaultPenWidth)}",

                        style = MaterialTheme.typography.labelLarge,

                        color = MaterialTheme.colorScheme.onSurfaceVariant

                    )

                    Slider(state.settings.defaultPenWidth, viewModel::setDefaultPenWidth, valueRange = 1f..100f, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp))

                    Text(

                        "Silgi boyutu: ${"%.0f".format(state.settings.defaultEraserSize)}",

                        style = MaterialTheme.typography.labelLarge,

                        color = MaterialTheme.colorScheme.onSurfaceVariant

                    )

                    Slider(state.settings.defaultEraserSize, viewModel::setDefaultEraserSize, valueRange = 12f..70f, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp))

                    Text(

                        "Maksimum yakınlaştırma: ${"%.1f".format(state.settings.canvasMaxZoom)}x",

                        style = MaterialTheme.typography.labelLarge,

                        color = MaterialTheme.colorScheme.onSurfaceVariant

                    )

                    Slider(state.settings.canvasMaxZoom, viewModel::setCanvasMaxZoom, valueRange = 2f..8f, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp))

                }

            }

            2 -> {

                // AI & Backend

                SamsungSettingsSection("Not Yapılandırması") {

                    SettingInfoRow(Icons.Default.Description, "Varsayılan kayıt", "Otomatik kaydetme açık")

                    SettingSwitchRow(

                        Icons.Default.AutoAwesome,

                        "LLM ile düzenleme",

                        "Not oluşturma tercihleri",

                        state.settings.llmEnabled,

                        viewModel::setLlmEnabled

                    )

                    Spacer(Modifier.height(10.dp))

                    Text(

                        "Model 2 önceliği",

                        style = MaterialTheme.typography.labelLarge,

                        fontWeight = FontWeight.SemiBold,

                        color = MaterialTheme.colorScheme.onSurfaceVariant

                    )

                    Spacer(Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {

                        ModeButton("OCR", state.settings.enhancementMode == EnhancementMode.Ocr, Modifier.weight(1f)) {

                            viewModel.setEnhancementMode(EnhancementMode.Ocr)

                        }

                        ModeButton("Beyaz sayfa", state.settings.enhancementMode == EnhancementMode.VisualNote, Modifier.weight(1f)) {

                            viewModel.setEnhancementMode(EnhancementMode.VisualNote)

                        }

                    }

                    Spacer(Modifier.height(12.dp))

                    Text(

                        "Tahta hassasiyeti: ${"%.2f".format(state.settings.threshold)}",

                        style = MaterialTheme.typography.labelLarge,

                        color = MaterialTheme.colorScheme.onSurfaceVariant

                    )

                    Slider(state.settings.threshold, viewModel::setThreshold, valueRange = 0.45f..0.60f, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp))

                }

                Spacer(Modifier.height(12.dp))

                SamsungSettingsSection("Backend") {

                    SettingSwitchRow(

                        Icons.Default.AutoAwesome,

                        "FastAPI pipeline",

                        "Tahtadan ekle akışında backend kullanılır",

                        state.settings.useBackendPipeline,

                        viewModel::setUseBackendPipeline

                    )

                    Spacer(Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {

                        ModeButton("ADB tüneli", state.settings.backendBaseUrl == ADB_REVERSE_BACKEND_BASE_URL, Modifier.weight(1f)) {

                            viewModel.setBackendBaseUrl(ADB_REVERSE_BACKEND_BASE_URL)

                        }

                        ModeButton("Wi-Fi IP", state.settings.backendBaseUrl == LAN_BACKEND_BASE_URL, Modifier.weight(1f)) {

                            viewModel.setBackendBaseUrl(LAN_BACKEND_BASE_URL)

                        }

                        ModeButton("Emulator", state.settings.backendBaseUrl == EMULATOR_BACKEND_BASE_URL, Modifier.weight(1f)) {

                            viewModel.setBackendBaseUrl(EMULATOR_BACKEND_BASE_URL)

                        }

                    }

                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(

                        value = state.settings.backendBaseUrl,

                        onValueChange = viewModel::setBackendBaseUrl,

                        label = { Text("FastAPI base URL") },

                        supportingText = { Text("ADB: $ADB_REVERSE_BACKEND_BASE_URL | Wi-Fi: $LAN_BACKEND_BASE_URL") },

                        modifier = Modifier.fillMaxWidth(),

                        singleLine = true,

                        shape = RoundedCornerShape(14.dp)

                    )

                }

            }

            3 -> {

                // Sistem & Geliştirici

                SamsungSettingsSection("Geliştirici") {

                    SettingSwitchRow(Icons.Default.BugReport, "Debug modu", "Pipeline çıktıları ve tanılama", state.settings.debugMode, viewModel::setDebugMode)

                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(

                        value = state.settings.groqApiKey,

                        onValueChange = viewModel::setGroqApiKey,

                        label = { Text("Groq API anahtarı") },

                        modifier = Modifier.fillMaxWidth(),

                        singleLine = true,

                        shape = RoundedCornerShape(14.dp)

                    )

                    Spacer(Modifier.height(10.dp))

                    Text("OCR motoru", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)

                    OcrEngineChoice.entries.forEach { choice ->

                        TextButton(onClick = { viewModel.setOcrEngineChoice(choice) }, modifier = Modifier.fillMaxWidth()) {

                            Text(if (state.settings.ocrEngineChoice == choice) "✓ ${choice.name}" else choice.name)

                        }

                    }

                }

                Spacer(Modifier.height(12.dp))

                SamsungSettingsSection("Hakkında & Bilgi") {

                    SettingActionRow(Icons.Default.Info, "Uygulama bilgisi", "Sürüm ve proje bilgileri", onAbout)

                    SettingInfoRow(Icons.Default.Description, "Sürüm", "0.9.0")

                }

            }

        }

        Spacer(Modifier.height(22.dp))

    }

}



@Composable

private fun SamsungSettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {

    Surface(

        modifier = Modifier.fillMaxWidth(),

        shape = RoundedCornerShape(18.dp),

        color = MaterialTheme.colorScheme.surface,

        border = BorderStroke(1.dp, darkAwareColor(Color(0xFFE2E8F0), MaterialTheme.colorScheme.outlineVariant)),

        tonalElevation = 0.dp

    ) {

        Column(Modifier.padding(14.dp)) {

            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

            Spacer(Modifier.height(10.dp))

            content()

        }

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



        SettingsSection(title = "Backend API") {

            SettingSwitchRow(

                Icons.Default.AutoAwesome,

                "FastAPI pipeline",

                "Not içinden yapılan taramalarda PyTorch backend kullanılır",

                state.settings.useBackendPipeline,

                viewModel::setUseBackendPipeline

            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(

                value = state.settings.backendBaseUrl,

                onValueChange = viewModel::setBackendBaseUrl,

                label = { Text("FastAPI base URL") },

                supportingText = { Text("Kablosuz ADB: $ADB_REVERSE_BACKEND_BASE_URL  |  Wi-Fi: $LAN_BACKEND_BASE_URL") },

                modifier = Modifier.fillMaxWidth(),

                singleLine = true,

                shape = RoundedCornerShape(14.dp)

            )

            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {

                ModeButton(

                    "ADB tüneli",

                    state.settings.backendBaseUrl == DEFAULT_BACKEND_BASE_URL,

                    Modifier.weight(1f)

                ) {

                    viewModel.setBackendBaseUrl(DEFAULT_BACKEND_BASE_URL)

                }

                ModeButton(

                    "Wi-Fi IP",

                    state.settings.backendBaseUrl == LAN_BACKEND_BASE_URL,

                    Modifier.weight(1f)

                ) {

                    viewModel.setBackendBaseUrl(LAN_BACKEND_BASE_URL)

                }

            }

            Spacer(Modifier.height(8.dp))

            TextButton(onClick = { viewModel.setBackendBaseUrl(EMULATOR_BACKEND_BASE_URL) }) {

                Text(if (state.settings.backendBaseUrl == EMULATOR_BACKEND_BASE_URL) "✓ Emulator: $EMULATOR_BACKEND_BASE_URL" else "Emulator: $EMULATOR_BACKEND_BASE_URL")

            }

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

            SettingInfoRow(Icons.Default.Description, "Sürüm bilgisi", "0.9.0 ürün sürümü")

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

        border = BorderStroke(1.dp, darkAwareColor(Color(0xFFE2E8F0), MaterialTheme.colorScheme.outlineVariant))

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

        border = BorderStroke(1.dp, darkAwareColor(Color(0xFFE2E8F0), MaterialTheme.colorScheme.outlineVariant))

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

        color = darkAwareColor(Color(0xFFF8FAFC), MaterialTheme.colorScheme.surfaceVariant),

        border = BorderStroke(1.dp, darkAwareColor(Color(0xFFE2E8F0), MaterialTheme.colorScheme.outlineVariant))

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

        Spacer(Modifier.height(32.dp))

        Column(

            horizontalAlignment = Alignment.CenterHorizontally,

            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)

        ) {

            // Centered circular B2N Logo

            Image(

                painter = painterResource(id = board2NoteAppbarLogoRes()),

                contentDescription = "B2N Logo",

                modifier = Modifier

                    .width(160.dp)

                    .height(72.dp)

            )

            Spacer(Modifier.height(18.dp))

            Text(

                "Board2Note",

                style = MaterialTheme.typography.headlineMedium,

                fontWeight = FontWeight.Bold,

                color = MaterialTheme.colorScheme.onBackground

            )

            Spacer(Modifier.height(12.dp))

            Text(

                "Tahta görüntülerini yapay zeka desteğiyle düzenlenebilir ders notlarına dönüştüren premium Android asistanınız.",

                style = MaterialTheme.typography.bodyMedium,

                color = MaterialTheme.colorScheme.onSurfaceVariant,

                lineHeight = 22.sp,

                textAlign = androidx.compose.ui.text.style.TextAlign.Center,

                modifier = Modifier.padding(horizontal = 12.dp)

            )

            Spacer(Modifier.height(28.dp))

        }



        Surface(

            modifier = Modifier.fillMaxWidth(),

            shape = RoundedCornerShape(24.dp),

            color = MaterialTheme.colorScheme.surface,

            tonalElevation = 1.dp,

            border = BorderStroke(1.dp, darkAwareColor(Color(0xFFEFF6FF), MaterialTheme.colorScheme.outlineVariant)),

            shadowElevation = 2.dp

        ) {

            Column(Modifier.padding(16.dp)) {

                AboutInfoLine("Sürüm", "0.9.3", Icons.Default.Info)

                AboutInfoLine("Gizlilik", "Notlar ve görseller cihazınızın yerel depolama alanında güvenle saklanır.", Icons.Default.Description, showDivider = false)

            }

        }

        Spacer(Modifier.height(24.dp))

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

        Divider(color = darkAwareColor(Color(0xFFE2E8F0), MaterialTheme.colorScheme.outlineVariant))

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

private fun HeaderLogoMark(modifier: Modifier = Modifier) {

    Image(

        painter = painterResource(id = board2NoteAppbarLogoRes()),

        contentDescription = "Board2Note",

        contentScale = ContentScale.Fit,

        modifier = modifier

            .width(116.dp)

            .height(42.dp)

    )

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

                else -> darkAwareColor(Color(0xFFE2E8F0), MaterialTheme.colorScheme.outlineVariant)

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

            FileThumbnail(note.previewImagePath())

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

private fun PrimaryActionButton(

    text: String,

    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,

    modifier: Modifier = Modifier,

    onClick: () -> Unit

) {

    Button(

        onClick = onClick,

        modifier = modifier.height(52.dp),

        shape = RoundedCornerShape(16.dp),

        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)

    ) {

        icon?.let {

            Icon(it, contentDescription = null, modifier = Modifier.size(19.dp))

            Spacer(Modifier.width(8.dp))

        }

        Text(text, fontWeight = FontWeight.SemiBold)

    }

}



@Composable

private fun SecondaryActionButton(

    text: String,

    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,

    modifier: Modifier = Modifier,

    onClick: () -> Unit

) {

    FilledTonalButton(

        onClick = onClick,

        modifier = modifier.height(48.dp),

        shape = RoundedCornerShape(15.dp),

        colors = ButtonDefaults.filledTonalButtonColors(

            containerColor = darkAwareColor(Color(0xFFF1F5F9), MaterialTheme.colorScheme.surfaceVariant),

            contentColor = MaterialTheme.colorScheme.secondary

        )

    ) {

        icon?.let {

            Icon(it, contentDescription = null, modifier = Modifier.size(18.dp))

            Spacer(Modifier.width(7.dp))

        }

        Text(text, fontWeight = FontWeight.SemiBold)

    }

}



@Composable

private fun DangerOutlineButton(

    text: String,

    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,

    modifier: Modifier = Modifier,

    onClick: () -> Unit

) {

    OutlinedButton(

        onClick = onClick,

        modifier = modifier.height(48.dp),

        shape = RoundedCornerShape(15.dp),

        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary),

        border = BorderStroke(1.dp, Color(0xFFCBD5E1))

    ) {

        icon?.let {

            Icon(it, contentDescription = null, modifier = Modifier.size(18.dp))

            Spacer(Modifier.width(7.dp))

        }

        Text(text, fontWeight = FontWeight.SemiBold)

    }

}



@Composable

private fun CompactPillButton(

    text: String,

    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,

    onClick: () -> Unit

) {

    Surface(

        modifier = Modifier.clickable(onClick = onClick),

        shape = RoundedCornerShape(999.dp),

        color = darkAwareColor(Color(0xFFEFF6FF), MaterialTheme.colorScheme.primaryContainer),

        border = BorderStroke(1.dp, darkAwareColor(Color(0xFFDBEAFE), MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)))

    ) {

        Row(

            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),

            verticalAlignment = Alignment.CenterVertically

        ) {

            icon?.let {

                Icon(it, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))

                Spacer(Modifier.width(5.dp))

            }

            Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)

        }

    }

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

    var pendingDelete by remember { mutableStateOf<SavedNote?>(null) }



    pendingDelete?.let { note ->

        ConfirmDeleteNoteDialog(

            onDismiss = { pendingDelete = null },

            onConfirm = {

                pendingDelete = null

                viewModel.deleteNoteById(note.id)

            }

        )

    }



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

            text = { Text("Seçilen notlar çöp kutusuna taşınacak. Daha sonra geri yükleyebilirsiniz.") },

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

            border = BorderStroke(1.dp, darkAwareColor(Color(0xFFE2E8F0), MaterialTheme.colorScheme.outlineVariant))

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

                if (selectionMode) {

                    Surface(

                        modifier = Modifier.fillMaxWidth(),

                        shape = RoundedCornerShape(18.dp),

                        color = darkAwareColor(Color(0xFFEFF6FF), MaterialTheme.colorScheme.primaryContainer),

                        border = BorderStroke(1.dp, darkAwareColor(Color(0xFFDBEAFE), MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)))

                    ) {

                        Row(

                            modifier = Modifier.padding(10.dp),

                            horizontalArrangement = Arrangement.spacedBy(10.dp),

                            verticalAlignment = Alignment.CenterVertically

                        ) {

                            Text(

                                "${selectedIds.size} not seçildi",

                                modifier = Modifier.weight(1f),

                                style = MaterialTheme.typography.labelLarge,

                                fontWeight = FontWeight.Bold,

                                color = MaterialTheme.colorScheme.primary

                            )

                            DangerOutlineButton("Sil", Icons.Default.Delete, Modifier.weight(1f)) {

                                if (selectedIds.isNotEmpty()) deleteMode = FolderDeleteMode.Selected

                            }

                            SecondaryActionButton("Vazgeç", null, Modifier.weight(1f)) {

                                selectionMode = false

                                selectedIds = emptySet()

                            }

                        }

                    }

                    Spacer(Modifier.height(10.dp))

                    DangerOutlineButton(

                        text = "Tümünü Sil",

                        icon = Icons.Default.Delete,

                        modifier = Modifier.fillMaxWidth()

                    ) { deleteMode = FolderDeleteMode.All }

                } else {

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {

                        SecondaryActionButton(

                            text = "Seç",

                            icon = Icons.Default.Check,

                            modifier = Modifier.weight(1f),

                            onClick = { if (notes.isNotEmpty()) selectionMode = true }

                        )

                        DangerOutlineButton(

                            text = "Tümünü Sil",

                            icon = Icons.Default.Delete,

                            modifier = Modifier.weight(1f),

                            onClick = { if (notes.isNotEmpty()) deleteMode = FolderDeleteMode.All }

                        )

                    }

                }

            }

        }



        Column(modifier = Modifier.padding(horizontal = 16.dp)) {

            if (notes.isEmpty()) {

                FolderEmptyState()

            } else {

                notes.forEach { note ->

                    SwipeableNoteActions(

                        onArchive = { viewModel.archiveNote(note.id) },

                        onDelete = { pendingDelete = note },

                        isVertical = false

                    ) {

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

                    }

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

        border = BorderStroke(1.dp, darkAwareColor(Color(0xFFE2E8F0), MaterialTheme.colorScheme.outlineVariant))

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

        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else darkAwareColor(Color(0xFFE2E8F0), MaterialTheme.colorScheme.outlineVariant))

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

        shape = RoundedCornerShape(22.dp),

        color = MaterialTheme.colorScheme.surface,

        tonalElevation = 2.dp,

        shadowElevation = 1.dp,

        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

    ) {

        Column {

            // Stacked Folder Silhouette Area

            Box(

                modifier = Modifier

                    .fillMaxWidth()

                    .height(130.dp)

                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),

                contentAlignment = Alignment.Center

            ) {

                // Folder Back

                Box(

                    modifier = Modifier

                        .width(96.dp)

                        .height(78.dp)

                        .offset(y = 12.dp)

                        .background(

                            Brush.verticalGradient(

                                colors = listOf(color.copy(alpha = 0.75f), color)

                            ),

                            shape = RoundedCornerShape(12.dp)

                        )

                ) {

                    // Folder Tab

                    Box(

                        modifier = Modifier

                            .width(36.dp)

                            .height(14.dp)

                            .offset(x = 8.dp, y = (-6).dp)

                            .background(color.copy(alpha = 0.75f), RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))

                    )

                }



                // Peeking Document sheet

                Surface(

                    modifier = Modifier

                        .width(76.dp)

                        .height(70.dp)

                        .offset(y = (-2).dp)

                        .graphicsLayer(rotationZ = -3f),

                    shape = RoundedCornerShape(6.dp),

                    color = Color.White,

                    shadowElevation = 3.dp,

                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))

                ) {

                    if (bitmap != null) {

                        Image(

                            bitmap = bitmap.asImageBitmap(),

                            contentDescription = null,

                            modifier = Modifier.fillMaxSize(),

                            contentScale = ContentScale.Crop

                        )

                    } else {

                        // Document lines mock graphic

                        Column(

                            modifier = Modifier

                                .fillMaxSize()

                                .padding(8.dp),

                            verticalArrangement = Arrangement.spacedBy(4.dp)

                        ) {

                            Icon(

                                Icons.Default.Description,

                                contentDescription = null,

                                tint = color.copy(alpha = 0.2f),

                                modifier = Modifier.size(16.dp)

                            )

                            Spacer(Modifier.height(2.dp))

                            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Color(0xFFE2E8F0)))

                            Box(modifier = Modifier.fillMaxWidth(0.7f).height(2.dp).background(Color(0xFFE2E8F0)))

                        }

                    }

                }



                // Folder Front Cover

                Box(

                    modifier = Modifier

                        .width(96.dp)

                        .height(52.dp)

                        .offset(y = 25.dp)

                        .shadow(4.dp, RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp, topStart = 4.dp, topEnd = 4.dp))

                        .background(

                            Brush.verticalGradient(

                                colors = listOf(color, color.copy(alpha = 0.9f))

                            ),

                            shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp, topStart = 4.dp, topEnd = 4.dp)

                        ),

                    contentAlignment = Alignment.Center

                ) {

                    Text(

                        text = initial,

                        style = MaterialTheme.typography.titleMedium,

                        fontWeight = FontWeight.Bold,

                        color = Color.White.copy(alpha = 0.95f)

                    )

                }

            }



            // Folder details below

            Column(modifier = Modifier.padding(14.dp, 12.dp, 14.dp, 14.dp)) {

                Text(

                    text = courseName,

                    fontWeight = FontWeight.SemiBold,

                    maxLines = 1,

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

                        text = "$noteCount not",

                        style = MaterialTheme.typography.labelSmall,

                        color = color,

                        fontWeight = FontWeight.Bold,

                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)

                    )

                }

            }

        }

    }

}

@Composable
private fun CourseFolderPopup(
    courseName: String,
    notes: List<SavedNote>,
    onDismiss: () -> Unit,
    onOpenNote: (SavedNote) -> Unit,
    onCreateNote: () -> Unit,
    onDeleteCourse: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var visible by remember(courseName) { mutableStateOf(false) }
    var showDeleteCourseDialog by remember(courseName) { mutableStateOf(false) }
    val courseColor = if (courseName == "Genel") Color(0xFF64748B) else folderColor(courseName)
    val sortedNotes = remember(notes) { notes.sortedByDescending { it.updatedAtEpochMs } }
    val canDeleteCourse = !courseName.equals("Genel", ignoreCase = true)

    fun closeWithAnimation() {
        visible = false
        scope.launch {
            delay(170)
            onDismiss()
        }
    }

    LaunchedEffect(courseName) {
        visible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = if (visible) 0.22f else 0f))
            .clickable { closeWithAnimation() },
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(150)) + scaleIn(
                animationSpec = tween(190),
                initialScale = 0.92f
            ),
            exit = fadeOut(tween(120)) + scaleOut(
                animationSpec = tween(150),
                targetScale = 0.96f
            )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.84f)
                    .widthIn(max = 420.dp)
                    .heightIn(max = 460.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 5.dp,
                shadowElevation = 12.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(38.dp),
                            shape = RoundedCornerShape(13.dp),
                            color = courseColor.copy(alpha = 0.14f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = courseColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = courseName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${sortedNotes.size} not",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (canDeleteCourse) {
                            IconButton(onClick = { showDeleteCourseDialog = true }, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Dersi sil",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        IconButton(onClick = { closeWithAnimation() }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Kapat")
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    if (sortedNotes.isEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Description,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "Bu derste henuz not yok",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Yeni not olusturup bu derse atayabilirsin.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            sortedNotes.take(6).forEach { note ->
                                FolderPopupNoteRow(
                                    note = note,
                                    courseColor = courseColor,
                                    onClick = { onOpenNote(note) }
                                )
                            }
                            if (sortedNotes.size > 6) {
                                Text(
                                    text = "+${sortedNotes.size - 6} not daha",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { closeWithAnimation() }) {
                            Text("Kapat")
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = onCreateNote, shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Yeni not")
                        }
                    }
                }
            }
        }
    }

    if (showDeleteCourseDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteCourseDialog = false },
            title = { Text("Dersi sil") },
            text = {
                Text(
                    "'$courseName' dersi silinecek. Notlar silinmez; Genel klasörüne taşınır."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteCourseDialog = false
                        onDeleteCourse()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Sil")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteCourseDialog = false }) {
                    Text("Vazgeç")
                }
            }
        )
    }
}

@Composable
private fun FolderPopupNoteRow(
    note: SavedNote,
    courseColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val previewPath = note.previewImagePath()
            val bitmap = remember(previewPath) { previewPath?.let { BitmapFactory.decodeFile(it) } }
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(12.dp),
                color = courseColor.copy(alpha = 0.12f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = if (note.noteType == NoteType.Canvas) Icons.Default.Image else Icons.Default.Description,
                            contentDescription = null,
                            tint = courseColor,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.title.ifBlank { "Basliksiz not" },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = formatDate(note.updatedAtEpochMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}



@Composable

private fun NoteGridCard(

    note: SavedNote,

    onClick: () -> Unit,

    modifier: Modifier = Modifier

) {

    val previewPath = note.previewImagePath()

    val bitmap = remember(previewPath) {

        previewPath?.let { BitmapFactory.decodeFile(it) }

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

    customCourseColors[courseName]?.let { return it }

    val palette = listOf(

        Color(0xFF2563EB),

        Color(0xFF1D4ED8),

        Color(0xFF0F172A),

        Color(0xFF334155),

        Color(0xFF475569),

        Color(0xFF1E40AF)

    )

    return palette[Math.floorMod(courseName.hashCode(), palette.size)]

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

    route == Route.Home -> ""

    route == Route.Capture -> ""

    route == Route.Review -> "Görsel Önizleme"

    route == Route.Crop -> "Tahta Alanı Seçimi"

    route == Route.Enhancement -> "Görüntü İyileştirme"

    route == Route.Ocr -> "OCR"

    route == Route.NoteEditor -> "Not Editörü"

    route == Route.Notes -> ""

    route?.startsWith(Route.NoteDetail) == true -> "Not Detayı"

    route?.startsWith(Route.Folder) == true -> "Ders Notları"

    route == Route.Settings -> ""

    route == Route.About -> "Hakkında"

    route == Route.Archive -> "Arşiv"

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



private fun String.stripCanvasMarkers(): String =
    substringBefore("[[B2N_CANVAS_V2]]")
        .substringBefore("[CanvasPages:")
        .substringBefore("[DrawingData:")
        .substringBefore("[[B2N_TEXT_V1]]")
        .trim()

private fun FormattedNote.toShareText(): String = buildString {

    appendLine(title)

    if (courseName.isNotBlank()) appendLine(courseName)

    appendLine()

    append(body.stripCanvasMarkers())

}



private fun SavedNote.toShareText(): String = buildString {

    appendLine(title)

    if (courseName.isNotBlank()) appendLine(courseName)

    appendLine()

    val clean = body.stripCanvasMarkers()
    if (clean.isNotEmpty()) {
        append(clean)
    } else if (noteType == NoteType.Canvas) {
        append("Canvas Çizim Notu")
    }

}



private fun SavedNote.previewImagePath(): String? =

    if (noteType == NoteType.Canvas) {

        canvasImagePath ?: whitePageImagePath ?: cropImagePath

    } else {

        whitePageImagePath ?: cropImagePath

    }



private fun formatDate(epochMs: Long): String =

    SimpleDateFormat("dd MMM yyyy HH:mm", Locale("tr", "TR")).format(Date(epochMs))



@Composable

private fun FavoritesScreen(

    state: Board2NotesUiState,

    viewModel: Board2NotesViewModel,

    onOpen: (SavedNote) -> Unit

) {

    var pendingDelete by remember { mutableStateOf<SavedNote?>(null) }

    pendingDelete?.let { note ->

        ConfirmDeleteNoteDialog(

            onDismiss = { pendingDelete = null },

            onConfirm = {

                pendingDelete = null

                viewModel.deleteNoteById(note.id)

            }

        )

    }



    ScreenColumn {

        Spacer(Modifier.height(18.dp))

        Surface(

            modifier = Modifier.fillMaxWidth(),

            shape = RoundedCornerShape(24.dp),

            color = MaterialTheme.colorScheme.surface,

            tonalElevation = 1.dp,

            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)

        ) {

            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {

                Surface(

                    modifier = Modifier.size(48.dp),

                    shape = RoundedCornerShape(16.dp),

                    color = MaterialTheme.colorScheme.primaryContainer

                ) {

                    Box(contentAlignment = Alignment.Center) {

                        Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)

                    }

                }

                Spacer(Modifier.width(14.dp))

                Column {

                    Text("Favoriler", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

                    Text(

                        "${state.favoriteNotes.size} favori not",

                        style = MaterialTheme.typography.bodyMedium,

                        color = MaterialTheme.colorScheme.onSurfaceVariant

                    )

                }

            }

        }

        Spacer(Modifier.height(16.dp))

        if (state.favoriteNotes.isEmpty()) {

            EmptyState("Favori not yok.", "Yıldız eklediğiniz notlar burada görünür.")

        } else {

            state.favoriteNotes.forEach { note ->

                FavoriteNoteCard(

                    note = note,

                    onOpen = { onOpen(note) },

                    onToggleFavorite = { viewModel.toggleFavorite(note.id) },

                    onDelete = { pendingDelete = note }

                )

                Spacer(Modifier.height(12.dp))

            }

        }

        Spacer(Modifier.height(18.dp))

    }

}



@Composable

private fun FavoriteNoteCard(

    note: SavedNote,

    onOpen: () -> Unit,

    onToggleFavorite: () -> Unit,

    onDelete: () -> Unit

) {

    Surface(

        modifier = Modifier.fillMaxWidth(),

        shape = RoundedCornerShape(20.dp),

        color = MaterialTheme.colorScheme.surface,

        tonalElevation = 1.dp,

        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)

    ) {

        Column(Modifier.padding(14.dp)) {

            Row(

                modifier = Modifier

                    .fillMaxWidth()

                    .clickable(onClick = onOpen),

                verticalAlignment = Alignment.CenterVertically

            ) {

                Column(modifier = Modifier.weight(1f)) {

                    Text(note.title.ifBlank { "Başlıksız not" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                    Spacer(Modifier.height(5.dp))

                    Text(formatDate(note.updatedAtEpochMs), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)

                    Spacer(Modifier.height(7.dp))

                    Text(note.preview.ifBlank { "Boş not" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)

                }

                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B))

            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {

                SecondaryActionButton("Favoriden Çıkar", Icons.Default.Star, Modifier.weight(1f), onToggleFavorite)

                DangerOutlineButton("Sil", Icons.Default.Delete, Modifier.weight(1f), onDelete)

            }

        }

    }

}



@Composable

private fun TrashScreen(

    state: Board2NotesUiState,

    viewModel: Board2NotesViewModel,

    onOpen: (SavedNote) -> Unit

) {

    var pendingPermanentDelete by remember { mutableStateOf<SavedNote?>(null) }

    var showEmptyTrashDialog by remember { mutableStateOf(false) }



    if (pendingPermanentDelete != null) {

        AlertDialog(

            onDismissRequest = { pendingPermanentDelete = null },

            shape = RoundedCornerShape(24.dp),

            title = { Text("Kalıcı Olarak Sil") },

            text = { Text("Bu not kalıcı olarak silinecek ve geri alınamayacak. Devam etmek istiyor musunuz?") },

            confirmButton = {

                Button(

                    onClick = {

                        pendingPermanentDelete?.let { viewModel.deleteNotePermanently(it.id) }

                        pendingPermanentDelete = null

                    }

                ) { Text("Sil") }

            },

            dismissButton = {

                TextButton(onClick = { pendingPermanentDelete = null }) { Text("Vazgeç") }

            }

        )

    }



    if (showEmptyTrashDialog) {

        AlertDialog(

            onDismissRequest = { showEmptyTrashDialog = false },

            shape = RoundedCornerShape(24.dp),

            title = { Text("Çöp Kutusunu Boşalt") },

            text = { Text("Çöp kutusundaki tüm notlar kalıcı olarak silinecek ve kurtarılamayacak. Devam etmek istiyor musunuz?") },

            confirmButton = {

                Button(

                    onClick = {

                        viewModel.emptyTrash()

                        showEmptyTrashDialog = false

                    }

                ) { Text("Boşalt") }

            },

            dismissButton = {

                TextButton(onClick = { showEmptyTrashDialog = false }) { Text("Vazgeç") }

            }

        )

    }



    ScreenColumn {

        Spacer(Modifier.height(18.dp))

        Surface(

            modifier = Modifier.fillMaxWidth(),

            shape = RoundedCornerShape(24.dp),

            color = MaterialTheme.colorScheme.surface,

            tonalElevation = 1.dp,

            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)

        ) {

            Row(

                modifier = Modifier.padding(18.dp),

                verticalAlignment = Alignment.CenterVertically,

                horizontalArrangement = Arrangement.SpaceBetween

            ) {

                Row(verticalAlignment = Alignment.CenterVertically) {

                    Surface(

                        modifier = Modifier.size(48.dp),

                        shape = RoundedCornerShape(16.dp),

                        color = MaterialTheme.colorScheme.errorContainer

                    ) {

                        Box(contentAlignment = Alignment.Center) {

                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)

                        }

                    }

                    Spacer(Modifier.width(14.dp))

                    Column {

                        Text("Çöp Kutusu", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

                        Text(

                            "${state.deletedNotes.size} not çöp kutusunda",

                            style = MaterialTheme.typography.bodyMedium,

                            color = MaterialTheme.colorScheme.onSurfaceVariant

                        )

                    }

                }

                if (state.deletedNotes.isNotEmpty()) {

                    Button(

                        onClick = { showEmptyTrashDialog = true },

                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)

                    ) {

                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))

                        Spacer(Modifier.width(8.dp))

                        Text("Çöpü Boşalt")

                    }

                }

            }

        }

        Spacer(Modifier.height(16.dp))

        if (state.deletedNotes.isEmpty()) {

            EmptyState("Çöp kutusu boş.", "Silinen notlar çöp kutusunda saklanır.")

        } else {

            state.deletedNotes.forEach { note ->

                TrashNoteCard(

                    note = note,

                    onRestore = { viewModel.restoreNote(note.id) },

                    onDeletePermanently = { pendingPermanentDelete = note }

                )

                Spacer(Modifier.height(12.dp))

            }

        }

        Spacer(Modifier.height(18.dp))

    }

}



@Composable

private fun TrashNoteCard(

    note: SavedNote,

    onRestore: () -> Unit,

    onDeletePermanently: () -> Unit

) {

    Surface(

        modifier = Modifier.fillMaxWidth(),

        shape = RoundedCornerShape(20.dp),

        color = MaterialTheme.colorScheme.surface,

        tonalElevation = 1.dp,

        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)

    ) {

        Column(Modifier.padding(14.dp)) {

            Row(

                modifier = Modifier.fillMaxWidth(),

                verticalAlignment = Alignment.CenterVertically

            ) {

                Column(modifier = Modifier.weight(1f)) {

                    Text(note.title.ifBlank { "Başlıksız not" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                    Spacer(Modifier.height(5.dp))

                    Text(formatDate(note.updatedAtEpochMs), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)

                    Spacer(Modifier.height(7.dp))

                    Text(note.preview.ifBlank { "Boş not" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)

                }

                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)

            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {

                SecondaryActionButton("Geri Yükle", Icons.Default.Refresh, Modifier.weight(1f), onRestore)

                DangerOutlineButton("Kalıcı Olarak Sil", Icons.Default.Delete, Modifier.weight(1f), onDeletePermanently)

            }

        }

    }

}

