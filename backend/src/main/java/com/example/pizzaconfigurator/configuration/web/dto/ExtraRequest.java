package com.example.pizzaconfigurator.configuration.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record ExtraRequest(@NotBlank String ingredientCode, @PositiveOrZero int quantity) {
}
