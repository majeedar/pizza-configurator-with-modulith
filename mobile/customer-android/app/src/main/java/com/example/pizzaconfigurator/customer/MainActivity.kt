package com.example.pizzaconfigurator.customer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.pizzaconfigurator.customer.ui.nav.PizzaNavGraph
import com.example.pizzaconfigurator.customer.ui.theme.PizzaConfiguratorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PizzaConfiguratorTheme {
                PizzaNavGraph()
            }
        }
    }
}
