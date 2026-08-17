package com.example.pizzaconfigurator.recommendation.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record PatchExtraRequest(@NotBlank String ingredientCode, @PositiveOrZero int quantity) {
}
