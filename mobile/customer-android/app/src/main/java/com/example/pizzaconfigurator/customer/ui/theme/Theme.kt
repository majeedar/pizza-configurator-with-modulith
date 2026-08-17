package com.example.pizzaconfigurator.customer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PizzaColorScheme = lightColorScheme(
    primary = Color(0xFFC62828),
    secondary = Color(0xFFFFF8E1),
    background = Color(0xFFFFFBFE)
)

@Composable
fun PizzaConfiguratorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PizzaColorScheme,
        content = content
    )
}
