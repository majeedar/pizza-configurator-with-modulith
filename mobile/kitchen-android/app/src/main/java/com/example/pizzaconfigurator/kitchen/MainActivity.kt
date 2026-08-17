package com.example.pizzaconfigurator.kitchen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.pizzaconfigurator.kitchen.ui.nav.KitchenNavGraph
import com.example.pizzaconfigurator.kitchen.ui.theme.PizzaKitchenTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PizzaKitchenTheme {
                KitchenNavGraph()
            }
        }
    }
}
