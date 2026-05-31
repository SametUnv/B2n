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
    primary = Color(0xFF3B82F6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFEFF6FF),
    secondary = Color(0xFFF8FAFC),
    onSecondary = Color(0xFF0F172A),
    tertiary = Color(0xFF94A3B8),
    background = Color(0xFF0F172A), // Premium dark navy black
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF1E293B), // Premium dark slate gray surface
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFF94A3B8),
    error = Color(0xFFF87171)
)

@Composable
fun Board2NotesTheme(useDarkTheme: Boolean = false, content: @Composable () -> Unit) {
    val colors = if (useDarkTheme) Board2NotesDarkColors else Board2NotesLightColors
    MaterialTheme(
        colorScheme = colors,
        typography = MaterialTheme.typography,
        content = content
    )
}
