package com.example.pizzaconfigurator.customer.data.dto

import java.math.BigDecimal
import kotlinx.serialization.Serializable

@Serializable
data class BasketItemView(
    val basketItemId: String,
    val configurationId: String,
    val quantity: Int,
    val pizzaName: String,
    val sizeCode: String,
    val doughCode: String,
    @Serializable(with = BigDecimalSerializer::class) val unitPrice: BigDecimal,
    val currency: String,
    @Serializable(with = BigDecimalSerializer::class) val lineTotal: BigDecimal
)

@Serializable
data class BasketView(
    val basketId: String,
    val sessionToken: String,
    val items: List<BasketItemView> = emptyList(),
    @Serializable(with = BigDecimalSerializer::class) val total: BigDecimal,
    val currency: String
)

@Serializable
data class AddBasketItemRequest(
    val configurationId: String,
    val quantity: Int
)
