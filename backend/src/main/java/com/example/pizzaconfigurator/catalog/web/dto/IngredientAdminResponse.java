package com.example.pizzaconfigurator.catalog.web.dto;

import com.example.pizzaconfigurator.catalog.domain.IngredientType;
import java.util.UUID;

public record IngredientAdminResponse(
    UUID ingredientId,
    String code,
    String name,
    IngredientType type,
    boolean active,
    String defaultUnit
) {
}
