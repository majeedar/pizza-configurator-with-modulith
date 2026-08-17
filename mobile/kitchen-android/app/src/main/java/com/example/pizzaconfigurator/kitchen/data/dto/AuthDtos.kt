package com.example.pizzaconfigurator.kitchen.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class StaffLoginRequest(val username: String, val password: String)

@Serializable
data class StaffLoginResponse(
    val employeeId: String,
    val username: String,
    val displayName: String,
    val role: String,
    val token: String
)
