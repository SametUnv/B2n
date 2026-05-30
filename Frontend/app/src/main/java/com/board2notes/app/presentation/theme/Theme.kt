package com.board2notes.app.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Board2NotesColors: ColorScheme = lightColorScheme(
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

@Composable
fun Board2NotesTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Board2NotesColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
