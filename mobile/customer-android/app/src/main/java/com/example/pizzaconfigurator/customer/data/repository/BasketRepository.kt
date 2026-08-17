package com.example.pizzaconfigurator.customer.data.repository

import com.example.pizzaconfigurator.customer.data.dto.AddBasketItemRequest
import com.example.pizzaconfigurator.customer.data.dto.BasketView
import com.example.pizzaconfigurator.customer.data.local.BasketIdStore
import com.example.pizzaconfigurator.customer.data.remote.ApiService
import com.example.pizzaconfigurator.customer.data.remote.apiCall

/**
 * Mirrors `BasketContext.tsx`: reuse the persisted basket id if the backend still recognizes it,
 * otherwise create a fresh basket. A stale/unknown id (e.g. after a backend reset) falls through
 * to creation rather than surfacing an error to the user.
 */
class BasketRepository(private val api: ApiService, private val basketIdStore: BasketIdStore) {

    suspend fun loadOrCreate(bearerToken: String?): BasketView {
        val storedId = basketIdStore.currentOrNull()
        if (storedId != null) {
            try {
                return apiCall { api.getBasket(storedId) }
            } catch (e: Exception) {
                // fall through to creating a new basket
            }
        }
        val basket = apiCall { api.createBasket(bearerToken?.let { "Bearer $it" }) }
        basketIdStore.save(basket.basketId)
        return basket
    }

    suspend fun addItem(basketId: String, configurationId: String, quantity: Int): BasketView =
        apiCall { api.addBasketItem(basketId, AddBasketItemRequest(configurationId, quantity)) }

    suspend fun removeItem(basketId: String, basketItemId: String): BasketView =
        apiCall { api.removeBasketItem(basketId, basketItemId) }

    suspend fun get(basketId: String): BasketView = apiCall { api.getBasket(basketId) }
}
