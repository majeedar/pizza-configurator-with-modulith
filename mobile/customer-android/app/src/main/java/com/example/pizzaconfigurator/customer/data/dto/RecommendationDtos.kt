package com.example.pizzaconfigurator.customer.data.dto

import kotlinx.serialization.Serializable

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

/** Parsed client-side from ReviewRequestView.proposedModificationJson. */
@Serializable
data class ProposedModificationSnapshot(
    val removedIngredientCodes: List<String> = emptyList(),
    val extras: List<ExtraSelection> = emptyList(),
    val sizeCode: String,
    val doughCode: String
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
data class ReviewOutcome(
    val reviewRequest: ReviewRequestView,
    val session: ConfigurationSessionView
)
