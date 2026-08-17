package com.example.pizzaconfigurator.kitchen.data.repository

import com.example.pizzaconfigurator.kitchen.data.dto.StaffLoginRequest
import com.example.pizzaconfigurator.kitchen.data.local.StaffAuthStore
import com.example.pizzaconfigurator.kitchen.data.remote.ApiService
import com.example.pizzaconfigurator.kitchen.data.remote.apiCall
import kotlinx.coroutines.flow.first

class AuthRepository(private val api: ApiService, private val authStore: StaffAuthStore) {

    val authState = authStore.authState

    suspend fun login(username: String, password: String) {
        val response = apiCall { api.login(StaffLoginRequest(username, password)) }
        authStore.save(response)
    }

    suspend fun logout() = authStore.clear()

    suspend fun bearerHeaderOrThrow(): String {
        val token = authState.first().token ?: error("Not signed in")
        return "Bearer $token"
    }

    suspend fun tokenOrNull(): String? = authState.first().token
}
