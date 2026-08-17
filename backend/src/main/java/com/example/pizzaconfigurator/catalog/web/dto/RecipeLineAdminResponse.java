package com.example.pizzaconfigurator.catalog.web.dto;

import java.util.UUID;

public record RecipeLineAdminResponse(
    UUID pizzaIngredientId,
    String ingredientCode,
    String ingredientName,
    int defaultQuantity,
    boolean removable
) {
}
