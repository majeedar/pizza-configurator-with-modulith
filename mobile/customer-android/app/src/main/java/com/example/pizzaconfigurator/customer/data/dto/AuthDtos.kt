package com.example.pizzaconfigurator.customer.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val name: String,
    val email: String,
    val phoneNumber: String? = null,
    val password: String
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val customerId: String,
    val name: String,
    val email: String,
    val token: String
)
