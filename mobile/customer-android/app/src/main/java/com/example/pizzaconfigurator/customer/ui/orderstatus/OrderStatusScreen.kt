package com.example.pizzaconfigurator.customer.ui.orderstatus

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pizzaconfigurator.customer.ui.rememberAppContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderStatusScreen(displayNumber: String, guestAccessToken: String?) {
    val container = rememberAppContainer()
    val viewModel: OrderStatusViewModel = viewModel(
        factory = OrderStatusViewModel.factory(displayNumber, guestAccessToken, container.orderRepository, container.authStore)
    )
    val state by viewModel.uiState.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Order $displayNumber") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            when {
                state.loading -> CircularProgressIndicator()
                state.error != null -> Text("Couldn't load order status: ${state.error}")
                state.order != null -> {
                    val order = state.order!!
                    Text("Status: ${order.status}", style = androidx.compose.material3.MaterialTheme.typography.headlineSmall)
                    Text("Pickup code: ${order.pickupToken}")
                    Text("Total: ${order.totalPrice} ${order.currency}")
                }
            }
        }
    }
}
