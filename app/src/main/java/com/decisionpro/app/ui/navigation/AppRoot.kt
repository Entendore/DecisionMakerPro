package com.decisionpro.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.decisionpro.app.ui.screens.home.HomeScreen
import com.decisionpro.app.ui.screens.marblerun.MarbleRunScreen
import com.decisionpro.app.ui.screens.matrix.MatrixScreen
import com.decisionpro.app.ui.screens.tree.TreeScreen

object Routes {
    const val HOME = "home"
    const val MATRIX = "matrix"
    const val TREE = "tree"
    const val MARBLE_RUN = "marble_run"
}

@Composable
fun AppRoot() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onOpenMatrix = { navController.navigate(Routes.MATRIX) },
                onOpenTree = { navController.navigate(Routes.TREE) },
                onOpenMarbleRun = { navController.navigate(Routes.MARBLE_RUN) }
            )
        }
        composable(Routes.MATRIX) {
            MatrixScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Routes.TREE) {
            TreeScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Routes.MARBLE_RUN) {
            MarbleRunScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
