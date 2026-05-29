package com.board2notes.app.presentation.navigation

/** All navigation destinations and the parent graph route. */
sealed class Screen(val route: String) {
    data object Welcome : Screen("welcome")
    data object Home : Screen("home")
    data object Preview : Screen("preview")
    data object Processing : Screen("processing")
    data object Enhancement : Screen("enhancement")
    data object OcrNote : Screen("ocr_note")
    data object VisualNote : Screen("visual_note")
    data object Export : Screen("export")

    companion object {
        const val GRAPH_ROUTE = "board_graph"
    }
}
