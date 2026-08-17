package com.example.pizzaconfigurator.kitchen.data.dto

import java.math.BigDecimal
import kotlinx.serialization.Serializable

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

/** The board only ever shows these four — orders past READY drop off, mirroring ProductionBoardPage.tsx. */
enum class BoardColumn(val status: String, val label: String) {
    CONFIRMED("CONFIRMED", "Confirmed"),
    APPROVED("APPROVED", "Approved"),
    IN_PROCESSING("IN_PROCESSING", "In processing"),
    READY("READY", "Ready")
}

/** Mirrors ProductionBoardPage.tsx's NEXT_ACTION map: the one command that advances a given status. */
fun nextActionFor(status: String): String? = when (status) {
    "CONFIRMED" -> "approve"
    "APPROVED" -> "start"
    "IN_PROCESSING" -> "ready"
    "READY" -> "complete"
    else -> null
}
