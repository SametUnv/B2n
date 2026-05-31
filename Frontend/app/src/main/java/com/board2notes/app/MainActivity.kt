package com.board2notes.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.board2notes.app.presentation.screens.Board2NotesApp
import com.board2notes.app.presentation.state.Board2NotesViewModel
import com.board2notes.app.presentation.theme.Board2NotesTheme
import androidx.compose.runtime.collectAsState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        val app = application as Board2NotesApplication
        setContent {
            val viewModel: Board2NotesViewModel = viewModel(
                factory = Board2NotesViewModel.Factory(app)
            )
            val uiState = viewModel.uiState.collectAsState().value
            Board2NotesTheme(useDarkTheme = uiState.settings.useDarkTheme) {
                Board2NotesApp(viewModel = viewModel)
            }
        }
    }
}
