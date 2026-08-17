package com.example.pizzaconfigurator.kitchen.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.pizzaconfigurator.kitchen.KitchenApp
import com.example.pizzaconfigurator.kitchen.di.AppContainer

@Composable
fun rememberAppContainer(): AppContainer {
    val context = LocalContext.current.applicationContext as KitchenApp
    return context.container
}
