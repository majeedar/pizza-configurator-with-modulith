package com.example.pizzaconfigurator.kitchen.ui.reviews

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.pizzaconfigurator.kitchen.data.dto.ConfigurationPatchRequest
import com.example.pizzaconfigurator.kitchen.data.dto.OriginalRequestSnapshot
import com.example.pizzaconfigurator.kitchen.data.dto.ReviewRequestView
import com.example.pizzaconfigurator.kitchen.data.remote.NetworkFactory
import com.example.pizzaconfigurator.kitchen.data.repository.ReviewRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.decodeFromString
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReviewCard(val review: ReviewRequestView, val original: OriginalRequestSnapshot?)

data class ReviewQueueUiState(
    val loading: Boolean = true,
    val cards: List<ReviewCard> = emptyList(),
    val error: String? = null
)

class ReviewQueueViewModel(private val reviewRepository: ReviewRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewQueueUiState())
    val uiState: StateFlow<ReviewQueueUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            try {
                val reviews = reviewRepository.listReviews()
                val cards = reviews.map { review ->
                    val original = runCatching {
                        NetworkFactory.json.decodeFromString<OriginalRequestSnapshot>(review.originalRequestJson)
                    }.getOrNull()
                    ReviewCard(review, original)
                }
                _uiState.value = ReviewQueueUiState(loading = false, cards = cards)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(loading = false, error = e.message)
            }
        }
    }

    fun accept(reviewId: String) = act { reviewRepository.accept(reviewId) }

    fun recommend(reviewId: String, sizeCode: String, doughCode: String, removedIngredientCodes: Set<String>) =
        act { reviewRepository.recommend(reviewId, ConfigurationPatchRequest(removedIngredientCodes, null, sizeCode, doughCode)) }

    fun reject(reviewId: String, reason: String?) = act { reviewRepository.reject(reviewId, reason) }

    private fun act(action: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                action()
                refresh()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    companion object {
        fun factory(reviewRepository: ReviewRepository) = viewModelFactory {
            initializer { ReviewQueueViewModel(reviewRepository) }
        }
    }
}
