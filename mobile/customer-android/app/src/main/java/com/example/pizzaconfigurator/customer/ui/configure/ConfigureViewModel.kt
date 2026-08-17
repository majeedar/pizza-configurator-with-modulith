package com.example.pizzaconfigurator.customer.ui.configure

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.pizzaconfigurator.customer.data.dto.ConfigurableOptions
import com.example.pizzaconfigurator.customer.data.dto.ConfigurationInput
import com.example.pizzaconfigurator.customer.data.dto.ExtraSelection
import com.example.pizzaconfigurator.customer.data.dto.PriceQuote
import com.example.pizzaconfigurator.customer.data.dto.Violation
import com.example.pizzaconfigurator.customer.data.local.AuthStore
import com.example.pizzaconfigurator.customer.data.repository.BasketRepository
import com.example.pizzaconfigurator.customer.data.repository.CatalogRepository
import com.example.pizzaconfigurator.customer.data.repository.ConfigurationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ConfigureUiState(
    val loading: Boolean = true,
    val options: ConfigurableOptions? = null,
    val sizeCode: String? = null,
    val doughCode: String? = null,
    val removedIngredients: Set<String> = emptySet(),
    val extras: Map<String, Int> = emptyMap(),
    val comment: String = "",
    val configurationId: String? = null,
    val validationStatus: String? = null,
    val priceStatus: String? = null,
    val violations: List<Violation> = emptyList(),
    val quote: PriceQuote? = null,
    val checking: Boolean = false,
    val addedToBasket: Boolean = false,
    val error: String? = null
) {
    val canAddToBasket: Boolean
        get() = configurationId != null &&
            (validationStatus == "VALID" || validationStatus == "REVIEW_APPROVED") &&
            (priceStatus == "PRICED" || priceStatus == "READY_FOR_CHECKOUT")

    val isPendingReview: Boolean get() = validationStatus == "PENDING_REVIEW"
    val isInvalid: Boolean get() = validationStatus == "INVALID"
}

class ConfigureViewModel(
    private val pizzaId: String,
    private val catalogRepository: CatalogRepository,
    private val configurationRepository: ConfigurationRepository,
    private val basketRepository: BasketRepository,
    private val authStore: AuthStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConfigureUiState())
    val uiState: StateFlow<ConfigureUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val options = catalogRepository.pizzaOptions(pizzaId)
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    options = options,
                    sizeCode = options.sizes.firstOrNull()?.code,
                    doughCode = options.doughs.firstOrNull()?.code
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(loading = false, error = e.message)
            }
        }
    }

    fun selectSize(code: String) { _uiState.value = _uiState.value.copy(sizeCode = code) }
    fun selectDough(code: String) { _uiState.value = _uiState.value.copy(doughCode = code) }
    fun updateComment(text: String) { _uiState.value = _uiState.value.copy(comment = text) }

    fun toggleRemoved(ingredientCode: String) {
        val current = _uiState.value.removedIngredients
        _uiState.value = _uiState.value.copy(
            removedIngredients = if (ingredientCode in current) current - ingredientCode else current + ingredientCode
        )
    }

    fun changeExtraQuantity(ingredientCode: String, delta: Int) {
        val current = _uiState.value.extras
        val next = ((current[ingredientCode] ?: 0) + delta).coerceAtLeast(0)
        _uiState.value = _uiState.value.copy(
            extras = if (next == 0) current - ingredientCode else current + (ingredientCode to next)
        )
    }

    fun checkAvailabilityAndPrice() {
        val state = _uiState.value
        val sizeCode = state.sizeCode ?: return
        val doughCode = state.doughCode ?: return

        viewModelScope.launch {
            _uiState.value = state.copy(checking = true, error = null)
            try {
                val input = ConfigurationInput(
                    pizzaId = pizzaId,
                    sizeCode = sizeCode,
                    doughCode = doughCode,
                    removedIngredients = state.removedIngredients.toList(),
                    extras = state.extras.map { (code, qty) -> ExtraSelection(code, qty) },
                    comment = state.comment.ifBlank { null }
                )

                val existingId = state.configurationId
                val session = if (existingId == null) {
                    val token = authStore.authState.first().token
                    configurationRepository.create(input, token)
                } else {
                    configurationRepository.update(existingId, input)
                }

                val validation = configurationRepository.validate(session.configurationId)

                var quote: PriceQuote? = null
                if (validation.session.validationStatus == "VALID") {
                    quote = configurationRepository.price(session.configurationId).quote
                }

                _uiState.value = _uiState.value.copy(
                    checking = false,
                    configurationId = session.configurationId,
                    validationStatus = validation.session.validationStatus,
                    priceStatus = quote?.let { "PRICED" } ?: validation.session.priceStatus,
                    violations = validation.violations,
                    quote = quote
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(checking = false, error = e.message)
            }
        }
    }

    fun addToBasket() {
        val state = _uiState.value
        val configurationId = state.configurationId ?: return
        viewModelScope.launch {
            try {
                val token = authStore.authState.first().token
                val basket = basketRepository.loadOrCreate(token)
                basketRepository.addItem(basket.basketId, configurationId, quantity = 1)
                _uiState.value = _uiState.value.copy(addedToBasket = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    companion object {
        fun factory(
            pizzaId: String,
            catalogRepository: CatalogRepository,
            configurationRepository: ConfigurationRepository,
            basketRepository: BasketRepository,
            authStore: AuthStore
        ) = viewModelFactory {
            initializer {
                ConfigureViewModel(pizzaId, catalogRepository, configurationRepository, basketRepository, authStore)
            }
        }
    }
}
