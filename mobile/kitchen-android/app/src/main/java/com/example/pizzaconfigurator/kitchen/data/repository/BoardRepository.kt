package com.example.pizzaconfigurator.kitchen.data.repository

import com.example.pizzaconfigurator.kitchen.data.dto.OrderView
import com.example.pizzaconfigurator.kitchen.data.remote.ApiService
import com.example.pizzaconfigurator.kitchen.data.remote.apiCall

class BoardRepository(private val api: ApiService, private val authRepository: AuthRepository) {

    suspend fun listOrders(): List<OrderView> = apiCall { api.listOrders(authRepository.bearerHeaderOrThrow()) }

    suspend fun runAction(orderId: String, action: String): OrderView = apiCall {
        val bearer = authRepository.bearerHeaderOrThrow()
        when (action) {
            "approve" -> api.approve(orderId, bearer)
            "start" -> api.start(orderId, bearer)
            "ready" -> api.ready(orderId, bearer)
            "complete" -> api.complete(orderId, bearer)
            else -> error("Unknown action: $action")
        }
    }
}
