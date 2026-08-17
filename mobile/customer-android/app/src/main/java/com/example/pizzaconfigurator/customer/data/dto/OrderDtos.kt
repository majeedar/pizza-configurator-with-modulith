package com.example.pizzaconfigurator.customer.data.dto

import java.math.BigDecimal
import kotlinx.serialization.Serializable

@Serializable
data class CreateOrderRequest(
    val basketId: String,
    val customNotes: String? = null,
    val fcmDeviceToken: String? = null
)

@Serializable
data class OrderItemView(
    val orderItemId: String,
    val pizzaId: String,
    val pizzaNameSnapshot: String,
    val sizeCode: String,
    val doughCode: String,
    val quantity: Int,
    val modificationsJson: String? = null,
    @Serializable(with = BigDecimalSerializer::class) val unitPrice: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class) val subtotal: BigDecimal
)

@Serializable
data class OrderView(
    val orderId: String,
    val displayNumber: String,
    val status: String,
    @Serializable(with = BigDecimalSerializer::class) val totalPrice: BigDecimal,
    val currency: String,
    val customerId: String? = null,
    val customNotes: String? = null,
    val pickupToken: String,
    val items: List<OrderItemView> = emptyList(),
    val createdAt: String
)

@Serializable
data class OrderCheckoutResponse(
    val order: OrderView,
    val accessToken: String? = null
)
