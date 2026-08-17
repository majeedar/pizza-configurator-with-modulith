package com.example.pizzaconfigurator.customer.ui.basket

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.pizzaconfigurator.customer.data.dto.BasketView
import com.example.pizzaconfigurator.customer.data.local.AuthStore
import com.example.pizzaconfigurator.customer.data.repository.BasketRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class BasketUiState(
    val loading: Boolean = true,
    val basket: BasketView? = null,
    val error: String? = null
)

class BasketViewModel(
    private val basketRepository: BasketRepository,
    private val authStore: AuthStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(BasketUiState())
    val uiState: StateFlow<BasketUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            try {
                val token = authStore.authState.first().token
                val basket = basketRepository.loadOrCreate(token)
                _uiState.value = _uiState.value.copy(loading = false, basket = basket)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(loading = false, error = e.message)
            }
        }
    }

    fun removeItem(basketItemId: String) {
        val basketId = _uiState.value.basket?.basketId ?: return
        viewModelScope.launch {
            try {
                val basket = basketRepository.removeItem(basketId, basketItemId)
                _uiState.value = _uiState.value.copy(basket = basket)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    companion object {
        fun factory(basketRepository: BasketRepository, authStore: AuthStore) = viewModelFactory {
            initializer { BasketViewModel(basketRepository, authStore) }
        }
    }
}
