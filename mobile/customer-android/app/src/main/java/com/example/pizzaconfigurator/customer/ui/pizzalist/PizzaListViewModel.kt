package com.example.pizzaconfigurator.customer.ui.pizzalist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.pizzaconfigurator.customer.data.dto.PizzaSummary
import com.example.pizzaconfigurator.customer.data.repository.CatalogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface PizzaListUiState {
    data object Loading : PizzaListUiState
    data class Success(val pizzas: List<PizzaSummary>) : PizzaListUiState
    data class Error(val message: String) : PizzaListUiState
}

class PizzaListViewModel(private val catalogRepository: CatalogRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<PizzaListUiState>(PizzaListUiState.Loading)
    val uiState: StateFlow<PizzaListUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = PizzaListUiState.Loading
            _uiState.value = try {
                PizzaListUiState.Success(catalogRepository.listPizzas())
            } catch (e: Exception) {
                PizzaListUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    companion object {
        fun factory(catalogRepository: CatalogRepository) = viewModelFactory {
            initializer { PizzaListViewModel(catalogRepository) }
        }
    }
}
