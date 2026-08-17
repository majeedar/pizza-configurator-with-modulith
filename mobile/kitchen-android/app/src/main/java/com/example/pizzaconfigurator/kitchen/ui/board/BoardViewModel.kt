package com.example.pizzaconfigurator.kitchen.ui.board

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.pizzaconfigurator.kitchen.data.dto.OrderView
import com.example.pizzaconfigurator.kitchen.data.remote.KitchenStream
import com.example.pizzaconfigurator.kitchen.data.repository.BoardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BoardUiState(
    val loading: Boolean = true,
    val orders: List<OrderView> = emptyList(),
    val error: String? = null,
    val actingOn: String? = null
)

/**
 * Live-updates the same way the staff web app's `ProductionBoardPage` does: any SSE event —
 * regardless of name/payload — is just a signal to refetch the full order list (agent.md §17,
 * "SSE is an optimization, not the only source of truth").
 */
class BoardViewModel(
    private val boardRepository: BoardRepository,
    private val kitchenStream: KitchenStream
) : ViewModel() {

    private val _uiState = MutableStateFlow(BoardUiState())
    val uiState: StateFlow<BoardUiState> = _uiState.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            kitchenStream.listen { refresh() }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                val orders = boardRepository.listOrders()
                _uiState.value = _uiState.value.copy(loading = false, orders = orders, error = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(loading = false, error = e.message)
            }
        }
    }

    fun runAction(orderId: String, action: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actingOn = orderId)
            try {
                boardRepository.runAction(orderId, action)
                refresh()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            } finally {
                _uiState.value = _uiState.value.copy(actingOn = null)
            }
        }
    }

    companion object {
        fun factory(boardRepository: BoardRepository, kitchenStream: KitchenStream) = viewModelFactory {
            initializer { BoardViewModel(boardRepository, kitchenStream) }
        }
    }
}
