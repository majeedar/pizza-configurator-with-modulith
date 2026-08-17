package com.example.pizzaconfigurator.customer.ui.configure

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pizzaconfigurator.customer.data.dto.RecipeItem
import com.example.pizzaconfigurator.customer.ui.rememberAppContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigureScreen(
    pizzaId: String,
    onAddedToBasket: () -> Unit,
    onCheckRecommendation: (configurationId: String) -> Unit,
    onBack: () -> Unit
) {
    val container = rememberAppContainer()
    val viewModel: ConfigureViewModel = viewModel(
        factory = ConfigureViewModel.factory(
            pizzaId,
            container.catalogRepository,
            container.configurationRepository,
            container.basketRepository,
            container.authStore
        )
    )
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.addedToBasket) {
        if (state.addedToBasket) onAddedToBasket()
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Configure pizza") }) }) { padding ->
        if (state.loading || state.options == null) {
            Column(
                Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) { CircularProgressIndicator() }
            return@Scaffold
        }
        val options = state.options!!

        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            item {
                Text("Base ingredients", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            }
            items(options.baseIngredients) { item -> BaseIngredientRow(item, state.removedIngredients.contains(item.ingredientCode), viewModel) }

            item {
                Text(
                    "Extras",
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
            items(options.availableExtras) { extra ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(extra.name)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.changeExtraQuantity(extra.code, -1) }) { Text("-") }
                        Text("${state.extras[extra.code] ?: 0}")
                        IconButton(onClick = { viewModel.changeExtraQuantity(extra.code, 1) }) { Text("+") }
                    }
                }
            }

            item {
                Text("Size", style = androidx.compose.material3.MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
                Row {
                    options.sizes.forEach { size ->
                        FilterChip(
                            selected = state.sizeCode == size.code,
                            onClick = { viewModel.selectSize(size.code) },
                            label = { Text(size.displayName) },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
            }

            item {
                Text("Dough", style = androidx.compose.material3.MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
                Row {
                    options.doughs.forEach { dough ->
                        FilterChip(
                            selected = state.doughCode == dough.code,
                            onClick = { viewModel.selectDough(dough.code) },
                            label = { Text(dough.displayName) },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = state.comment,
                    onValueChange = viewModel::updateComment,
                    label = { Text("Optional comment") },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                )
            }

            item {
                Button(
                    onClick = viewModel::checkAvailabilityAndPrice,
                    enabled = !state.checking,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                ) { Text(if (state.checking) "Checking..." else "Check availability & price") }
            }

            if (state.isInvalid) {
                items(state.violations) { violation ->
                    Text("• ${violation.message}", color = androidx.compose.material3.MaterialTheme.colorScheme.error)
                }
            }

            if (state.isPendingReview) {
                item {
                    Column(Modifier.padding(top = 8.dp)) {
                        Text(
                            "This comment needs kitchen review before we can price it. Remove the " +
                                "comment to continue now, or check back shortly for a recommendation."
                        )
                        state.configurationId?.let { configurationId ->
                            Button(
                                onClick = { onCheckRecommendation(configurationId) },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                            ) { Text("Check for a kitchen recommendation") }
                        }
                    }
                }
            }

            state.quote?.let { quote ->
                item {
                    Text(
                        "Total: ${quote.total} ${quote.currency}",
                        style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }

            item {
                Button(
                    onClick = viewModel::addToBasket,
                    enabled = state.canAddToBasket,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                ) { Text("Add to basket") }
            }

            state.error?.let { error ->
                item { Text(error, color = androidx.compose.material3.MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
private fun BaseIngredientRow(item: RecipeItem, removed: Boolean, viewModel: ConfigureViewModel) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (item.removable) {
            Checkbox(checked = !removed, onCheckedChange = { viewModel.toggleRemoved(item.ingredientCode) })
        }
        Text(item.ingredientName)
    }
}
