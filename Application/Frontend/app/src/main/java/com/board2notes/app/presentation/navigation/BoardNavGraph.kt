package com.board2notes.app.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.board2notes.app.di.AppModule
import com.board2notes.app.domain.model.BoardMode
import com.board2notes.app.presentation.enhancement.EnhancementScreen
import com.board2notes.app.presentation.export.ExportScreen
import com.board2notes.app.presentation.home.HomeScreen
import com.board2notes.app.presentation.ocrnote.OcrNoteScreen
import com.board2notes.app.presentation.preview.PreviewScreen
import com.board2notes.app.presentation.processing.ProcessingScreen
import com.board2notes.app.presentation.shared.BoardViewModel
import com.board2notes.app.presentation.visualnote.VisualNoteScreen
import com.board2notes.app.presentation.welcome.WelcomeScreen

private const val ANIM = 300

@Composable
fun BoardNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Welcome.route,
        route = Screen.GRAPH_ROUTE,
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(ANIM))
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(ANIM))
        },
        popEnterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(ANIM))
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(ANIM))
        }
    ) {
        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onStart = { navController.navigate(Screen.Home.route) }
            )
        }

        composable(Screen.Home.route) { entry ->
            val vm = entry.sharedViewModel(navController)
            HomeScreen(
                viewModel = vm,
                onImageReady = { navController.navigate(Screen.Preview.route) }
            )
        }

        composable(Screen.Preview.route) { entry ->
            val vm = entry.sharedViewModel(navController)
            PreviewScreen(
                viewModel = vm,
                onContinue = { navController.navigate(Screen.Processing.route) },
                onReselect = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Processing.route) { entry ->
            val vm = entry.sharedViewModel(navController)
            ProcessingScreen(
                viewModel = vm,
                onDone = {
                    navController.navigate(Screen.Enhancement.route) {
                        popUpTo(Screen.Processing.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Enhancement.route) { entry ->
            val vm = entry.sharedViewModel(navController)
            EnhancementScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onModeSelected = { mode ->
                    val dest = if (mode == BoardMode.OCR_NOTE) Screen.OcrNote.route
                    else Screen.VisualNote.route
                    navController.navigate(dest)
                }
            )
        }

        composable(Screen.OcrNote.route) { entry ->
            val vm = entry.sharedViewModel(navController)
            OcrNoteScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onExported = { navController.navigate(Screen.Export.route) }
            )
        }

        composable(Screen.VisualNote.route) { entry ->
            val vm = entry.sharedViewModel(navController)
            VisualNoteScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onExported = { navController.navigate(Screen.Export.route) }
            )
        }

        composable(Screen.Export.route) { entry ->
            val vm = entry.sharedViewModel(navController)
            ExportScreen(
                viewModel = vm,
                onHome = {
                    vm.resetFlow()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.GRAPH_ROUTE) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}

/**
 * Returns a [BoardViewModel] scoped to the parent nav graph, so the same
 * instance (and thus the same [com.board2notes.app.presentation.shared.BoardUiState])
 * is shared across all screens in the flow.
 */
@Composable
private fun NavBackStackEntry.sharedViewModel(navController: NavHostController): BoardViewModel {
    val parentRoute = destination.parent?.route ?: Screen.GRAPH_ROUTE
    val parentEntry = remember(this) { navController.getBackStackEntry(parentRoute) }
    return viewModel(
        viewModelStoreOwner = parentEntry,
        factory = BoardViewModel.Factory(AppModule.boardUseCases)
    )
}
