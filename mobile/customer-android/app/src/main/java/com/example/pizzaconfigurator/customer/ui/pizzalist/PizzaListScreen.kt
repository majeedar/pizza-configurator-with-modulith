package com.example.pizzaconfigurator.customer.ui.pizzalist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.pizzaconfigurator.customer.data.dto.PizzaSummary
import com.example.pizzaconfigurator.customer.ui.rememberAppContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PizzaListScreen(
    onPizzaSelected: (String) -> Unit,
    onBasketClick: () -> Unit,
    onAccountClick: () -> Unit
) {
    val container = rememberAppContainer()
    val viewModel: PizzaListViewModel = viewModel(
        factory = PizzaListViewModel.factory(container.catalogRepository)
    )
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pizza Configurator") },
                actions = {
                    IconButton(onClick = onAccountClick) {
                        Icon(Icons.Filled.Person, contentDescription = "Account")
                    }
                    IconButton(onClick = onBasketClick) {
                        Icon(Icons.Filled.ShoppingCart, contentDescription = "Basket")
                    }
                }
            )
        }
    ) { padding ->
        when (val current = state) {
            is PizzaListUiState.Loading ->
                Column(
                    Modifier.fillMaxSize().padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) { CircularProgressIndicator() }

            is PizzaListUiState.Error ->
                Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                    Text("Couldn't load the menu: ${current.message}")
                }

            is PizzaListUiState.Success ->
                LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                    items(current.pizzas) { pizza -> PizzaCard(pizza, onClick = { onPizzaSelected(pizza.pizzaId) }) }
                }
        }
    }
}

@Composable
private fun PizzaCard(pizza: PizzaSummary, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(pizza.name, style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
                Text("${pizza.basePrice}", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            }
            pizza.description?.let { Text(it, modifier = Modifier.padding(top = 4.dp)) }
        }
    }
}
