package com.example.pizzaconfigurator.customer.ui.checkout

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pizzaconfigurator.customer.ui.rememberAppContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    basketId: String,
    onOrderPlaced: (displayNumber: String, guestAccessToken: String?) -> Unit,
    onBack: () -> Unit
) {
    val container = rememberAppContainer()
    val viewModel: CheckoutViewModel = viewModel(
        factory = CheckoutViewModel.factory(basketId, container.orderRepository, container.authStore)
    )
    val uiState by viewModel.uiState.collectAsState()
    var customNotes by remember { mutableStateOf("") }

    // Requesting POST_NOTIFICATIONS here (rather than at app launch) keeps the ask in context —
    // right before the moment push would actually start mattering — and, per agent.md §8.5,
    // denial must not block placing the order: fetchTokenOrNull() degrades to null either way.
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result intentionally ignored — see comment above */ }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(uiState.placedDisplayNumber) {
        uiState.placedDisplayNumber?.let { onOrderPlaced(it, uiState.guestAccessToken) }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Checkout") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = customNotes,
                onValueChange = { customNotes = it },
                label = { Text("Notes for the kitchen (optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { viewModel.placeOrder(customNotes.ifBlank { null }) },
                enabled = !uiState.placing,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) { Text(if (uiState.placing) "Placing order..." else "Place order") }

            uiState.error?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error) }
        }
    }
}
