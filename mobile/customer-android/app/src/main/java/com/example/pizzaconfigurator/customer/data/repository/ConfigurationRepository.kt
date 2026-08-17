package com.example.pizzaconfigurator.customer.data.repository

import com.example.pizzaconfigurator.customer.data.dto.ConfigurationInput
import com.example.pizzaconfigurator.customer.data.dto.ConfigurationSessionView
import com.example.pizzaconfigurator.customer.data.dto.PriceResponse
import com.example.pizzaconfigurator.customer.data.dto.ReviewOutcome
import com.example.pizzaconfigurator.customer.data.dto.ReviewRequestView
import com.example.pizzaconfigurator.customer.data.dto.ValidationResponse
import com.example.pizzaconfigurator.customer.data.remote.ApiService
import com.example.pizzaconfigurator.customer.data.remote.apiCall

class ConfigurationRepository(private val api: ApiService) {

    suspend fun create(input: ConfigurationInput, bearerToken: String?): ConfigurationSessionView =
        apiCall { api.createConfiguration(input, bearerToken?.let { "Bearer $it" }) }

    suspend fun update(configurationId: String, input: ConfigurationInput): ConfigurationSessionView =
        apiCall { api.updateConfiguration(configurationId, input) }

    suspend fun validate(configurationId: String): ValidationResponse =
        apiCall { api.validateConfiguration(configurationId) }

    suspend fun price(configurationId: String): PriceResponse =
        apiCall { api.priceConfiguration(configurationId) }

    suspend fun get(configurationId: String): ConfigurationSessionView =
        apiCall { api.getConfiguration(configurationId) }

    /** Returns null rather than throwing when there's no RECOMMENDED_BY_KITCHEN proposal waiting (backend 404s). */
    suspend fun currentRecommendation(configurationId: String): ReviewRequestView? =
        try {
            apiCall { api.getRecommendation(configurationId) }
        } catch (e: com.example.pizzaconfigurator.customer.data.dto.ApiException) {
            if (e.status == 404) null else throw e
        }

    suspend fun acceptRecommendation(configurationId: String): ReviewOutcome =
        apiCall { api.acceptRecommendation(configurationId) }

    suspend fun rejectRecommendation(configurationId: String): ReviewOutcome =
        apiCall { api.rejectRecommendation(configurationId) }
}
