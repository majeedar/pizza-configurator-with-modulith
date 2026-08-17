package com.example.pizzaconfigurator.customer.data.repository

import com.example.pizzaconfigurator.customer.data.dto.ConfigurableOptions
import com.example.pizzaconfigurator.customer.data.dto.PizzaSummary
import com.example.pizzaconfigurator.customer.data.remote.ApiService
import com.example.pizzaconfigurator.customer.data.remote.apiCall

class CatalogRepository(private val api: ApiService) {
    suspend fun listPizzas(): List<PizzaSummary> = apiCall { api.listPizzas() }

    suspend fun pizzaOptions(pizzaId: String): ConfigurableOptions = apiCall { api.pizzaOptions(pizzaId) }
}
