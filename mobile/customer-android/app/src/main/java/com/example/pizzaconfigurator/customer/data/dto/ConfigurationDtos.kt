package com.example.pizzaconfigurator.customer.data.dto

import java.math.BigDecimal
import kotlinx.serialization.Serializable

@Serializable
data class ExtraSelection(
    val ingredientCode: String,
    val quantity: Int
)

@Serializable
data class ConfigurationInput(
    val pizzaId: String,
    val sizeCode: String,
    val doughCode: String,
    val removedIngredients: List<String>,
    val extras: List<ExtraSelection>,
    val comment: String? = null
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
data class Violation(
    val code: String,
    val field: String,
    val ruleCode: String? = null,
    val message: String
)

@Serializable
data class ConfigurationSuggestion(
    val description: String,
    val patch: Map<String, kotlinx.serialization.json.JsonElement> = emptyMap()
)

@Serializable
data class ValidationResponse(
    val session: ConfigurationSessionView,
    val violations: List<Violation> = emptyList(),
    val suggestions: List<ConfigurationSuggestion> = emptyList()
)

@Serializable
data class PriceQuote(
    val currency: String,
    @Serializable(with = BigDecimalSerializer::class) val base: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class) val size: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class) val dough: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class) val extras: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class) val total: BigDecimal,
    val priceVersion: String
)

@Serializable
data class PriceResponse(
    val session: ConfigurationSessionView,
    val quote: PriceQuote
)
