package com.example.pizzaconfigurator.customer.ui.recommendation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.example.pizzaconfigurator.customer.ui.rememberAppContainer

/**
 * Presents a kitchen recommendation distinctly and requires an explicit Accept or Reject
 * (agent.md §8.1 step 9) — never silently folded into the price/checkout flow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendationScreen(configurationId: String, onResolved: () -> Unit) {
    val container = rememberAppContainer()
    val viewModel: RecommendationViewModel = viewModel(
        factory = RecommendationViewModel.factory(configurationId, container.configurationRepository)
    )
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.responded) {
        if (state.responded) onResolved()
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Kitchen recommendation") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            when {
                state.loading -> CircularProgressIndicator()
                state.review == null -> Text("No recommendation is waiting for you on this configuration.")
                else -> {
                    Text("The kitchen proposed a change to your pizza:")
                    state.proposal?.let { proposal ->
                        Text("Size: ${proposal.sizeCode}, Dough: ${proposal.doughCode}", modifier = Modifier.padding(top = 8.dp))
                        if (proposal.removedIngredientCodes.isNotEmpty()) {
                            Text("Removed: ${proposal.removedIngredientCodes.joinToString()}")
                        }
                        if (proposal.extras.isNotEmpty()) {
                            Text("Extras: ${proposal.extras.joinToString { "${it.ingredientCode} x${it.quantity}" }}")
                        }
                    }
                    state.review?.reason?.let { Text(it, modifier = Modifier.padding(top = 8.dp)) }

                    Row(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                        Button(onClick = viewModel::accept, modifier = Modifier.padding(end = 8.dp)) { Text("Accept") }
                        Button(onClick = viewModel::reject) { Text("Reject") }
                    }
                }
            }
            state.error?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error) }
        }
    }
}
