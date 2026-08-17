package com.example.pizzaconfigurator.catalog.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record RecipeLineAdminRequest(
    @NotBlank String ingredientCode,
    @PositiveOrZero int defaultQuantity,
    boolean removable
) {
}
