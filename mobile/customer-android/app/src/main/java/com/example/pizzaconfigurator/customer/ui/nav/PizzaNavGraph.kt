package com.example.pizzaconfigurator.customer.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.pizzaconfigurator.customer.ui.auth.AuthScreen
import com.example.pizzaconfigurator.customer.ui.basket.BasketScreen
import com.example.pizzaconfigurator.customer.ui.checkout.CheckoutScreen
import com.example.pizzaconfigurator.customer.ui.configure.ConfigureScreen
import com.example.pizzaconfigurator.customer.ui.orderstatus.OrderStatusScreen
import com.example.pizzaconfigurator.customer.ui.pizzalist.PizzaListScreen
import com.example.pizzaconfigurator.customer.ui.recommendation.RecommendationScreen
import java.net.URLEncoder

private object Routes {
    const val PIZZA_LIST = "pizzas"
    const val CONFIGURE = "configure/{pizzaId}"
    const val BASKET = "basket"
    const val AUTH = "auth"
    const val CHECKOUT = "checkout/{basketId}"
    const val ORDER_STATUS = "orderStatus/{displayNumber}?token={token}"
    const val RECOMMENDATION = "recommendation/{configurationId}"
}

@Composable
fun PizzaNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.PIZZA_LIST) {
        composable(Routes.PIZZA_LIST) {
            PizzaListScreen(
                onPizzaSelected = { pizzaId -> navController.navigate("configure/$pizzaId") },
                onBasketClick = { navController.navigate(Routes.BASKET) },
                onAccountClick = { navController.navigate(Routes.AUTH) }
            )
        }
        composable(
            Routes.CONFIGURE,
            arguments = listOf(navArgument("pizzaId") { type = NavType.StringType })
        ) { backStackEntry ->
            val pizzaId = backStackEntry.arguments?.getString("pizzaId") ?: return@composable
            ConfigureScreen(
                pizzaId = pizzaId,
                onAddedToBasket = { navController.navigate(Routes.BASKET) },
                onCheckRecommendation = { configurationId -> navController.navigate("recommendation/$configurationId") },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.BASKET) {
            BasketScreen(
                onCheckout = { basketId -> navController.navigate("checkout/$basketId") },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.AUTH) {
            AuthScreen(onBack = { navController.popBackStack() })
        }
        composable(
            Routes.CHECKOUT,
            arguments = listOf(navArgument("basketId") { type = NavType.StringType })
        ) { backStackEntry ->
            val basketId = backStackEntry.arguments?.getString("basketId") ?: return@composable
            CheckoutScreen(
                basketId = basketId,
                onOrderPlaced = { displayNumber, guestAccessToken ->
                    val tokenPart = guestAccessToken?.let { "?token=${URLEncoder.encode(it, "UTF-8")}" } ?: ""
                    navController.navigate("orderStatus/$displayNumber$tokenPart") {
                        popUpTo(Routes.PIZZA_LIST)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            Routes.ORDER_STATUS,
            arguments = listOf(
                navArgument("displayNumber") { type = NavType.StringType },
                navArgument("token") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { backStackEntry ->
            val displayNumber = backStackEntry.arguments?.getString("displayNumber") ?: return@composable
            val token = backStackEntry.arguments?.getString("token")
            OrderStatusScreen(displayNumber = displayNumber, guestAccessToken = token)
        }
        composable(
            Routes.RECOMMENDATION,
            arguments = listOf(navArgument("configurationId") { type = NavType.StringType })
        ) { backStackEntry ->
            val configurationId = backStackEntry.arguments?.getString("configurationId") ?: return@composable
            RecommendationScreen(configurationId = configurationId, onResolved = { navController.popBackStack() })
        }
    }
}
