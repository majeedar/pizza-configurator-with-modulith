package com.example.pizzaconfigurator.kitchen.data.repository

import com.example.pizzaconfigurator.kitchen.data.dto.AppLinkView
import com.example.pizzaconfigurator.kitchen.data.remote.ApiService
import com.example.pizzaconfigurator.kitchen.data.remote.apiCall

class AppLinkRepository(private val api: ApiService, private val authRepository: AuthRepository) {
    suspend fun customerAppLink(): AppLinkView = apiCall { api.customerAppLink(authRepository.bearerHeaderOrThrow()) }
}
