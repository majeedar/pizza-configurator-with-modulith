package com.example.pizzaconfigurator.customer.di

import android.content.Context
import com.example.pizzaconfigurator.customer.data.local.AuthStore
import com.example.pizzaconfigurator.customer.data.local.BasketIdStore
import com.example.pizzaconfigurator.customer.data.remote.NetworkFactory
import com.example.pizzaconfigurator.customer.data.repository.AuthRepository
import com.example.pizzaconfigurator.customer.data.repository.BasketRepository
import com.example.pizzaconfigurator.customer.data.repository.CatalogRepository
import com.example.pizzaconfigurator.customer.data.repository.ConfigurationRepository
import com.example.pizzaconfigurator.customer.data.repository.OrderRepository

/**
 * Small hand-rolled service locator instead of Hilt/Dagger — this app has ~5 repositories and no
 * scoping complexity that would justify an annotation-processing framework (agent.md §33 "small
 * application services").
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val api = NetworkFactory.apiService

    val authStore = AuthStore(appContext)
    val basketIdStore = BasketIdStore(appContext)

    val authRepository = AuthRepository(api, authStore)
    val catalogRepository = CatalogRepository(api)
    val configurationRepository = ConfigurationRepository(api)
    val basketRepository = BasketRepository(api, basketIdStore)
    val orderRepository = OrderRepository(api)
}
