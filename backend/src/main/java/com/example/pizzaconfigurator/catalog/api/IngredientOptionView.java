package com.example.pizzaconfigurator.catalog.api;

import com.example.pizzaconfigurator.catalog.domain.IngredientType;

public record IngredientOptionView(
    String code,
    String name,
    IngredientType type,
    String defaultUnit
) {
}
