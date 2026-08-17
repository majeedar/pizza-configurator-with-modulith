package com.example.pizzaconfigurator.kitchen.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// High-contrast, touch-first palette for a kitchen display (agent.md §8.2).
private val KitchenColorScheme = lightColorScheme(
    primary = Color(0xFF1B5E20),
    secondary = Color(0xFFFFF3E0),
    background = Color(0xFFFFFFFF)
)

@Composable
fun PizzaKitchenTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = KitchenColorScheme, content = content)
}
