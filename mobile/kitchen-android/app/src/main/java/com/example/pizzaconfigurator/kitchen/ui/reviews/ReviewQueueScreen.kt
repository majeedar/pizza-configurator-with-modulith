package com.example.pizzaconfigurator.kitchen.ui.reviews

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pizzaconfigurator.kitchen.ui.rememberAppContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewQueueScreen(onBack: () -> Unit) {
    val container = rememberAppContainer()
    val viewModel: ReviewQueueViewModel = viewModel(factory = ReviewQueueViewModel.factory(container.reviewRepository))
    val state by viewModel.uiState.collectAsState()
    var recommendTargetReviewId by remember { mutableStateOf<String?>(null) }

    Scaffold(topBar = { TopAppBar(title = { Text("Review queue") }) }) { padding ->
        if (state.loading) {
            Column(Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally) { CircularProgressIndicator() }
            return@Scaffold
        }
        if (state.cards.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) { Text("No requests waiting for review.") }
            return@Scaffold
        }

        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            items(state.cards) { card ->
                Card(Modifier.fillMaxWidth().padding(12.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Review ${card.review.reviewRequestId.take(8)}", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                        card.original?.let {
                            Text("Size ${it.sizeCode}, dough ${it.doughCode}")
                            it.comment?.let { comment -> Text("Comment: \"$comment\"") }
                        }
                        card.review.reason?.let { Text(it, style = androidx.compose.material3.MaterialTheme.typography.bodySmall) }

                        Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            Button(
                                onClick = { viewModel.accept(card.review.reviewRequestId) },
                                modifier = Modifier.padding(end = 8.dp)
                            ) { Text("Accept") }
                            Button(
                                onClick = { recommendTargetReviewId = card.review.reviewRequestId },
                                modifier = Modifier.padding(end = 8.dp)
                            ) { Text("Recommend") }
                            Button(onClick = { viewModel.reject(card.review.reviewRequestId, null) }) { Text("Reject") }
                        }
                    }
                }
            }
        }

        state.error?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) }
    }

    val targetReviewId = recommendTargetReviewId
    if (targetReviewId != null) {
        val targetCard = state.cards.firstOrNull { it.review.reviewRequestId == targetReviewId }
        RecommendDialog(
            defaultSizeCode = targetCard?.original?.sizeCode.orEmpty(),
            defaultDoughCode = targetCard?.original?.doughCode.orEmpty(),
            onDismiss = { recommendTargetReviewId = null },
            onSubmit = { sizeCode, doughCode, removed ->
                viewModel.recommend(targetReviewId, sizeCode, doughCode, removed)
                recommendTargetReviewId = null
            }
        )
    }
}

@Composable
private fun RecommendDialog(
    defaultSizeCode: String,
    defaultDoughCode: String,
    onDismiss: () -> Unit,
    onSubmit: (sizeCode: String, doughCode: String, removedIngredientCodes: Set<String>) -> Unit
) {
    var sizeCode by remember { mutableStateOf(defaultSizeCode) }
    var doughCode by remember { mutableStateOf(defaultDoughCode) }
    var removedText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Propose a configuration") },
        text = {
            Column {
                OutlinedTextField(sizeCode, { sizeCode = it }, label = { Text("Size code") })
                OutlinedTextField(doughCode, { doughCode = it }, label = { Text("Dough code") }, modifier = Modifier.padding(top = 8.dp))
                OutlinedTextField(
                    removedText,
                    { removedText = it },
                    label = { Text("Removed ingredient codes (comma-separated)") },
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val removed = removedText.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
                onSubmit(sizeCode, doughCode, removed)
            }) { Text("Send to customer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
