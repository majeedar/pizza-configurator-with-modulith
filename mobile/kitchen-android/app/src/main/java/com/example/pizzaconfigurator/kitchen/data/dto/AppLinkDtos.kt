package com.example.pizzaconfigurator.kitchen.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class AppLinkView(
    val appLinkId: String,
    val platform: String,
    val audience: String,
    val url: String,
    val active: Boolean,
    val updatedBy: String? = null,
    val updatedAt: String
)
