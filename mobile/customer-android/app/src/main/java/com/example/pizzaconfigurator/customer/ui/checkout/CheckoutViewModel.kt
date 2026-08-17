package com.example.pizzaconfigurator.customer.ui.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.pizzaconfigurator.customer.data.local.AuthStore
import com.example.pizzaconfigurator.customer.data.repository.OrderRepository
import com.example.pizzaconfigurator.customer.push.PushTokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class CheckoutUiState(
    val placing: Boolean = false,
    val placedDisplayNumber: String? = null,
    val guestAccessToken: String? = null,
    val error: String? = null
)

class CheckoutViewModel(
    private val basketId: String,
    private val orderRepository: OrderRepository,
    private val authStore: AuthStore
) : ViewModel() {

    // Generated once per checkout attempt and reused across retries of *this* attempt
    // (agent.md §7.7 idempotency semantics) — a fresh key per tap would defeat the point.
    private val idempotencyKey = OrderRepository.newIdempotencyKey()

    private val _uiState = MutableStateFlow(CheckoutUiState())
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    fun placeOrder(customNotes: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(placing = true, error = null)
            try {
                val fcmToken = PushTokenManager.fetchTokenOrNull()
                val bearerToken = authStore.authState.first().token
                val response = orderRepository.createOrder(
                    idempotencyKey = idempotencyKey,
                    basketId = basketId,
                    customNotes = customNotes,
                    fcmDeviceToken = fcmToken,
                    bearerToken = bearerToken
                )
                _uiState.value = CheckoutUiState(
                    placedDisplayNumber = response.order.displayNumber,
                    guestAccessToken = response.accessToken
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(placing = false, error = e.message)
            }
        }
    }

    companion object {
        fun factory(basketId: String, orderRepository: OrderRepository, authStore: AuthStore) = viewModelFactory {
            initializer { CheckoutViewModel(basketId, orderRepository, authStore) }
        }
    }
}
