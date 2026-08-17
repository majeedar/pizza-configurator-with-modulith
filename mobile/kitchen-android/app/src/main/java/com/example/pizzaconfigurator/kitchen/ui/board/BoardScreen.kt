package com.example.pizzaconfigurator.kitchen.ui.board

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material3.Button
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.example.pizzaconfigurator.kitchen.data.dto.BoardColumn
import com.example.pizzaconfigurator.kitchen.data.dto.OrderView
import com.example.pizzaconfigurator.kitchen.data.dto.nextActionFor
import com.example.pizzaconfigurator.kitchen.ui.applink.AppLinkQrCorner
import com.example.pizzaconfigurator.kitchen.ui.rememberAppContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardScreen(onLogout: () -> Unit, onReviewsClick: () -> Unit) {
    val container = rememberAppContainer()
    val viewModel: BoardViewModel = viewModel(
        factory = BoardViewModel.factory(container.boardRepository, container.kitchenStream)
    )
    val state by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Production board") },
                actions = {
                    IconButton(onClick = onReviewsClick) {
                        Icon(Icons.Filled.RateReview, contentDescription = "Review queue")
                    }
                    IconButton(onClick = {
                        coroutineScope.launch {
                            container.authRepository.logout()
                            onLogout()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Log out")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.loading) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) { CircularProgressIndicator() }
                return@Scaffold
            }

            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
                BoardColumn.entries.forEach { column ->
                    OrderColumn(
                        column = column,
                        orders = state.orders.filter { it.status == column.status },
                        actingOn = state.actingOn,
                        onAction = { orderId, action -> viewModel.runAction(orderId, action) }
                    )
                }
            }

            AppLinkQrCorner(container.appLinkRepository)

            state.error?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp)) }
        }
    }
}

@Composable
private fun OrderColumn(
    column: BoardColumn,
    orders: List<OrderView>,
    actingOn: String?,
    onAction: (String, String) -> Unit
) {
    Column(Modifier.width(280.dp).fillMaxHeight().padding(8.dp)) {
        Text(column.label, style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
        LazyColumn {
            items(orders) { order -> OrderCard(order, isActing = actingOn == order.orderId, onAction = onAction) }
        }
    }
}

@Composable
private fun OrderCard(order: OrderView, isActing: Boolean, onAction: (String, String) -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(order.displayNumber, style = androidx.compose.material3.MaterialTheme.typography.headlineSmall)
            Text("${order.items.sumOf { it.quantity }} item(s) — ${order.totalPrice} ${order.currency}")
            order.customNotes?.let { Text(it, style = androidx.compose.material3.MaterialTheme.typography.bodySmall) }
            val action = nextActionFor(order.status)
            if (action != null) {
                Button(
                    onClick = { onAction(order.orderId, action) },
                    enabled = !isActing,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) { Text(action.replaceFirstChar { it.uppercase() }) }
            }
        }
    }
}
