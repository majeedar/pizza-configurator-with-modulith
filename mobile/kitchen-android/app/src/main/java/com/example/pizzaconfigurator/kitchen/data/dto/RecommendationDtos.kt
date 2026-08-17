package com.example.pizzaconfigurator.kitchen.data.dto

import java.math.BigDecimal
import kotlinx.serialization.Serializable

@Serializable
data class ExtraSelection(val ingredientCode: String, val quantity: Int)

/** Parsed client-side from ReviewRequestView.originalRequestJson (a raw JSON string on the wire). */
@Serializable
data class OriginalRequestSnapshot(
    val pizzaId: String,
    val sizeCode: String,
    val doughCode: String,
    val removedIngredientCodes: List<String> = emptyList(),
    val extras: List<ExtraSelection> = emptyList(),
    val comment: String? = null
)

@Serializable
data class ReviewRequestView(
    val reviewRequestId: String,
    val configurationId: String,
    val status: String,
    val reason: String? = null,
    val originalRequestJson: String,
    val proposedModificationJson: String? = null,
    val reviewedBy: String? = null,
    val reviewedAt: String? = null,
    val customerResponse: String? = null,
    val customerRespondedAt: String? = null,
    val createdAt: String
)

@Serializable
data class ConfigurationSessionView(
    val configurationId: String,
    val customerId: String? = null,
    val pizzaId: String,
    val sizeCode: String,
    val doughCode: String,
    val removedIngredientCodes: Set<String> = emptySet(),
    val extras: List<ExtraSelection> = emptyList(),
    val comment: String? = null,
    val validationStatus: String,
    val ruleVersion: String? = null,
    val priceStatus: String,
    @Serializable(with = BigDecimalSerializer::class) val calculatedPrice: BigDecimal? = null,
    val priceVersion: String? = null,
    val currency: String? = null,
    val expiresAt: String
)

@Serializable
data class ReviewOutcome(
    val reviewRequest: ReviewRequestView,
    val session: ConfigurationSessionView
)

@Serializable
data class PatchExtraRequest(val ingredientCode: String, val quantity: Int)

@Serializable
data class ConfigurationPatchRequest(
    val removedIngredientCodes: Set<String>? = null,
    val extras: List<PatchExtraRequest>? = null,
    val sizeCode: String,
    val doughCode: String
)

@Serializable
data class RejectRequest(val reason: String? = null)
