package com.example.pizzaconfigurator.customer.data.dto

import java.math.BigDecimal
import kotlinx.serialization.Serializable

@Serializable
data class PizzaSummary(
    val pizzaId: String,
    val code: String,
    val name: String,
    val description: String? = null,
    @Serializable(with = BigDecimalSerializer::class) val basePrice: BigDecimal
)

@Serializable
data class RecipeItem(
    val ingredientCode: String,
    val ingredientName: String,
    val defaultQuantity: Int,
    val removable: Boolean
)

@Serializable
data class IngredientOption(
    val code: String,
    val name: String,
    val type: String
)

@Serializable
data class SizeOption(
    val code: String,
    val displayName: String,
    @Serializable(with = BigDecimalSerializer::class) val priceModifier: BigDecimal
)

@Serializable
data class DoughOption(
    val code: String,
    val displayName: String,
    @Serializable(with = BigDecimalSerializer::class) val priceModifier: BigDecimal
)

@Serializable
data class ConfigurableOptions(
    val pizzaId: String,
    val baseIngredients: List<RecipeItem>,
    val availableExtras: List<IngredientOption>,
    val sizes: List<SizeOption>,
    val doughs: List<DoughOption>
)
