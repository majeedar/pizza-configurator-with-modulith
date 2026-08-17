package com.example.pizzaconfigurator.kitchen.data.repository

import com.example.pizzaconfigurator.kitchen.data.dto.ConfigurationPatchRequest
import com.example.pizzaconfigurator.kitchen.data.dto.RejectRequest
import com.example.pizzaconfigurator.kitchen.data.dto.ReviewOutcome
import com.example.pizzaconfigurator.kitchen.data.dto.ReviewRequestView
import com.example.pizzaconfigurator.kitchen.data.remote.ApiService
import com.example.pizzaconfigurator.kitchen.data.remote.apiCall

class ReviewRepository(private val api: ApiService, private val authRepository: AuthRepository) {

    suspend fun listReviews(): List<ReviewRequestView> = apiCall { api.listReviews(authRepository.bearerHeaderOrThrow()) }

    suspend fun accept(reviewId: String): ReviewOutcome = apiCall {
        api.acceptReview(reviewId, authRepository.bearerHeaderOrThrow())
    }

    suspend fun recommend(reviewId: String, patch: ConfigurationPatchRequest): ReviewOutcome = apiCall {
        api.recommendReview(reviewId, authRepository.bearerHeaderOrThrow(), patch)
    }

    suspend fun reject(reviewId: String, reason: String?): ReviewOutcome = apiCall {
        api.rejectReview(reviewId, authRepository.bearerHeaderOrThrow(), RejectRequest(reason))
    }
}
