package com.example.pizzaconfigurator.customer.ui.basket

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pizzaconfigurator.customer.data.dto.BasketItemView
import com.example.pizzaconfigurator.customer.ui.rememberAppContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BasketScreen(onCheckout: (String) -> Unit, onBack: () -> Unit) {
    val container = rememberAppContainer()
    val viewModel: BasketViewModel = viewModel(
        factory = BasketViewModel.factory(container.basketRepository, container.authStore)
    )
    val state by viewModel.uiState.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Basket") }) }) { padding ->
        if (state.loading) {
            Column(Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val basket = state.basket
        if (basket == null || basket.items.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                Text("Your basket is empty. Configure a pizza to add one.")
            }
            return@Scaffold
        }

        Column(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(Modifier.weight(1f)) {
                items(basket.items) { item -> BasketItemRow(item, onRemove = { viewModel.removeItem(item.basketItemId) }) }
            }
            HorizontalDivider()
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total: ${basket.total} ${basket.currency}", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
            }
            Button(
                onClick = { onCheckout(basket.basketId) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            ) { Text("Confirm & Checkout") }
        }

        state.error?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) }
    }
}

@Composable
private fun BasketItemRow(item: BasketItemView, onRemove: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("${item.pizzaName} (${item.sizeCode}, ${item.doughCode}) x${item.quantity}")
            Text("${item.lineTotal} ${item.currency}")
        }
        TextButton(onClick = onRemove) { Text("Remove") }
    }
}
