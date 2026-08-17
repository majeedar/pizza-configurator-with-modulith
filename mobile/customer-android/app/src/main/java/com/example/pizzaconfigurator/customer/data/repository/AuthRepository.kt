package com.example.pizzaconfigurator.customer.data.repository

import com.example.pizzaconfigurator.customer.data.dto.LoginRequest
import com.example.pizzaconfigurator.customer.data.dto.RegisterRequest
import com.example.pizzaconfigurator.customer.data.local.AuthStore
import com.example.pizzaconfigurator.customer.data.remote.ApiService
import com.example.pizzaconfigurator.customer.data.remote.apiCall

class AuthRepository(private val api: ApiService, private val authStore: AuthStore) {

    val authState = authStore.authState

    suspend fun register(name: String, email: String, phoneNumber: String?, password: String) {
        val response = apiCall { api.register(RegisterRequest(name, email, phoneNumber, password)) }
        authStore.save(response)
    }

    suspend fun login(email: String, password: String) {
        val response = apiCall { api.login(LoginRequest(email, password)) }
        authStore.save(response)
    }

    suspend fun logout() = authStore.clear()
}
