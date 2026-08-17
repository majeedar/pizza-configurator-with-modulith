package com.example.pizzaconfigurator.customer.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.pizzaconfigurator.customer.PizzaConfiguratorApp
import com.example.pizzaconfigurator.customer.di.AppContainer

@Composable
fun rememberAppContainer(): AppContainer {
    val context = LocalContext.current.applicationContext as PizzaConfiguratorApp
    return context.container
}
