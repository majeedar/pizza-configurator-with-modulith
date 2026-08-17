package com.example.pizzaconfigurator.customer.ui.recommendation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.pizzaconfigurator.customer.data.dto.ProposedModificationSnapshot
import com.example.pizzaconfigurator.customer.data.dto.ReviewRequestView
import com.example.pizzaconfigurator.customer.data.remote.NetworkFactory
import com.example.pizzaconfigurator.customer.data.repository.ConfigurationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.decodeFromString
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RecommendationUiState(
    val loading: Boolean = true,
    val review: ReviewRequestView? = null,
    val proposal: ProposedModificationSnapshot? = null,
    val responded: Boolean = false,
    val error: String? = null
)

class RecommendationViewModel(
    private val configurationId: String,
    private val configurationRepository: ConfigurationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecommendationUiState())
    val uiState: StateFlow<RecommendationUiState> = _uiState.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            try {
                val review = configurationRepository.currentRecommendation(configurationId)
                val proposal = review?.proposedModificationJson?.let {
                    runCatching { NetworkFactory.json.decodeFromString<ProposedModificationSnapshot>(it) }.getOrNull()
                }
                _uiState.value = RecommendationUiState(loading = false, review = review, proposal = proposal)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(loading = false, error = e.message)
            }
        }
    }

    fun accept() = respond { configurationRepository.acceptRecommendation(configurationId) }
    fun reject() = respond { configurationRepository.rejectRecommendation(configurationId) }

    private fun respond(action: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                action()
                _uiState.value = _uiState.value.copy(responded = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    companion object {
        fun factory(configurationId: String, configurationRepository: ConfigurationRepository) = viewModelFactory {
            initializer { RecommendationViewModel(configurationId, configurationRepository) }
        }
    }
}
