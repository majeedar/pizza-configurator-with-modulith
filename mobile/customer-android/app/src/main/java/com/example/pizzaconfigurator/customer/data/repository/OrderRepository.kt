package com.example.pizzaconfigurator.customer.data.repository

import com.example.pizzaconfigurator.customer.data.dto.CreateOrderRequest
import com.example.pizzaconfigurator.customer.data.dto.OrderCheckoutResponse
import com.example.pizzaconfigurator.customer.data.dto.OrderView
import com.example.pizzaconfigurator.customer.data.remote.ApiService
import com.example.pizzaconfigurator.customer.data.remote.apiCall
import java.util.UUID

class OrderRepository(private val api: ApiService) {

    /**
     * [idempotencyKey] must be generated once per checkout attempt by the caller and reused
     * across retries of the *same* attempt (agent.md §7.7) — a fresh UUID per call would defeat
     * the backend's replay protection.
     */
    suspend fun createOrder(
        idempotencyKey: String,
        basketId: String,
        customNotes: String?,
        fcmDeviceToken: String?,
        bearerToken: String?
    ): OrderCheckoutResponse = apiCall {
        api.createOrder(
            idempotencyKey = idempotencyKey,
            bearer = bearerToken?.let { "Bearer $it" },
            request = CreateOrderRequest(basketId, customNotes, fcmDeviceToken)
        )
    }

    suspend fun getStatus(displayNumber: String, bearerToken: String?, guestAccessToken: String?): OrderView =
        apiCall { api.getOrderStatus(displayNumber, bearerToken?.let { "Bearer $it" }, guestAccessToken) }

    companion object {
        fun newIdempotencyKey(): String = UUID.randomUUID().toString()
    }
}
