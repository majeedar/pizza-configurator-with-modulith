package com.example.pizzaconfigurator.kitchen.di

import android.content.Context
import com.example.pizzaconfigurator.kitchen.data.local.StaffAuthStore
import com.example.pizzaconfigurator.kitchen.data.remote.KitchenStream
import com.example.pizzaconfigurator.kitchen.data.remote.NetworkFactory
import com.example.pizzaconfigurator.kitchen.data.repository.AppLinkRepository
import com.example.pizzaconfigurator.kitchen.data.repository.AuthRepository
import com.example.pizzaconfigurator.kitchen.data.repository.BoardRepository
import com.example.pizzaconfigurator.kitchen.data.repository.ReviewRepository

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val api = NetworkFactory.apiService

    val authStore = StaffAuthStore(appContext)
    val authRepository = AuthRepository(api, authStore)
    val boardRepository = BoardRepository(api, authRepository)
    val reviewRepository = ReviewRepository(api, authRepository)
    val appLinkRepository = AppLinkRepository(api, authRepository)
    val kitchenStream = KitchenStream(bearerTokenProvider = { authRepository.tokenOrNull() })
}
