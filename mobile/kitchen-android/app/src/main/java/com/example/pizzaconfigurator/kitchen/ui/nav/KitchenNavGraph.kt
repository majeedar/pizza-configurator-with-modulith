package com.example.pizzaconfigurator.kitchen.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pizzaconfigurator.kitchen.ui.board.BoardScreen
import com.example.pizzaconfigurator.kitchen.ui.login.LoginScreen
import com.example.pizzaconfigurator.kitchen.ui.reviews.ReviewQueueScreen

private object Routes {
    const val LOGIN = "login"
    const val BOARD = "board"
    const val REVIEWS = "reviews"
}

@Composable
fun KitchenNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.LOGIN) {
        composable(Routes.LOGIN) {
            LoginScreen(onLoggedIn = {
                navController.navigate(Routes.BOARD) { popUpTo(Routes.LOGIN) { inclusive = true } }
            })
        }
        composable(Routes.BOARD) {
            BoardScreen(
                onLogout = { navController.navigate(Routes.LOGIN) { popUpTo(Routes.BOARD) { inclusive = true } } },
                onReviewsClick = { navController.navigate(Routes.REVIEWS) }
            )
        }
        composable(Routes.REVIEWS) {
            ReviewQueueScreen(onBack = { navController.popBackStack() })
        }
    }
}
