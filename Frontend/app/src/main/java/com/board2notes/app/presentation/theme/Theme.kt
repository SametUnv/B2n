package com.board2notes.app.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Board2NotesLightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF2563EB),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF0F172A),
    secondary = Color(0xFF0F172A),
    onSecondary = Color.White,
    tertiary = Color(0xFF64748B),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF64748B),
    error = Color(0xFFB3261E)
)

private val Board2NotesDarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFF60A5FA),
    onPrimary = Color(0xFF071A34),
    primaryContainer = Color(0xFF17325C),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFFA5B4FC),
    onSecondary = Color(0xFF172033),
    secondaryContainer = Color(0xFF29305A),
    onSecondaryContainer = Color(0xFFE0E7FF),
    tertiary = Color(0xFF5EEAD4),
    onTertiary = Color(0xFF042F2E),
    tertiaryContainer = Color(0xFF134E4A),
    onTertiaryContainer = Color(0xFFCCFBF1),
    background = Color(0xFF080D18),
    onBackground = Color(0xFFE5EDF8),
    surface = Color(0xFF101827),
    onSurface = Color(0xFFE5EDF8),
    surfaceVariant = Color(0xFF1A2536),
    onSurfaceVariant = Color(0xFFA9B7CA),
    surfaceTint = Color(0xFF60A5FA),
    inverseSurface = Color(0xFFE5EDF8),
    inverseOnSurface = Color(0xFF172033),
    inversePrimary = Color(0xFF2563EB),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF718096),
    outlineVariant = Color(0xFF334155),
    scrim = Color.Black
)

@Composable
fun Board2NotesTheme(useDarkTheme: Boolean = true, content: @Composable () -> Unit) {
    val colors = if (useDarkTheme) Board2NotesDarkColors else Board2NotesLightColors
    MaterialTheme(
        colorScheme = colors,
        typography = MaterialTheme.typography,
        content = content
    )
}
