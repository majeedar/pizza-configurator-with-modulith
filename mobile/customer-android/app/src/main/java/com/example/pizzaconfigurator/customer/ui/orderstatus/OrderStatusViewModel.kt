package com.example.pizzaconfigurator.customer.ui.orderstatus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.pizzaconfigurator.customer.data.dto.OrderView
import com.example.pizzaconfigurator.customer.data.local.AuthStore
import com.example.pizzaconfigurator.customer.data.repository.OrderRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val TERMINAL_STATUSES = setOf("COMPLETED", "CANCELLED", "REJECTED")
private const val POLL_INTERVAL_MS = 5000L

data class OrderStatusUiState(
    val loading: Boolean = true,
    val order: OrderView? = null,
    val error: String? = null
)

/**
 * Polls order status (agent.md §7.7's `GET /orders/{displayNumber}/status`) — the same
 * pull-based mechanism the order status page already uses, kept alongside push as a second,
 * always-available channel (push may never arrive: permission denied, no Play Services, token
 * fetch failure).
 */
class OrderStatusViewModel(
    private val displayNumber: String,
    private val guestAccessToken: String?,
    private val orderRepository: OrderRepository,
    private val authStore: AuthStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderStatusUiState())
    val uiState: StateFlow<OrderStatusUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            while (true) {
                val bearerToken = authStore.authState.first().token
                try {
                    val order = orderRepository.getStatus(displayNumber, bearerToken, guestAccessToken)
                    _uiState.value = OrderStatusUiState(loading = false, order = order)
                    if (order.status in TERMINAL_STATUSES) break
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(loading = false, error = e.message)
                    break
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    companion object {
        fun factory(
            displayNumber: String,
            guestAccessToken: String?,
            orderRepository: OrderRepository,
            authStore: AuthStore
        ) = viewModelFactory {
            initializer { OrderStatusViewModel(displayNumber, guestAccessToken, orderRepository, authStore) }
        }
    }
}
