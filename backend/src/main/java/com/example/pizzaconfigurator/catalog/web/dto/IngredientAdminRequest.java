package com.example.pizzaconfigurator.catalog.web.dto;

import com.example.pizzaconfigurator.catalog.domain.IngredientType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record IngredientAdminRequest(
    @NotBlank String code,
    @NotBlank String name,
    @NotNull IngredientType type,
    boolean active,
    String defaultUnit
) {
}
