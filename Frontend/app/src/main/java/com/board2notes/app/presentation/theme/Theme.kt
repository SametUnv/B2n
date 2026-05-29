package com.board2notes.app.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Board2NotesColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF126A73),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6F3F1),
    onPrimaryContainer = Color(0xFF063B40),
    secondary = Color(0xFF395B50),
    onSecondary = Color.White,
    tertiary = Color(0xFF765A16),
    background = Color(0xFFF7F8FA),
    onBackground = Color(0xFF1D2327),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1D2327),
    surfaceVariant = Color(0xFFE8ECEE),
    onSurfaceVariant = Color(0xFF465156),
    error = Color(0xFFB3261E)
)

@Composable
fun Board2NotesTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Board2NotesColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
