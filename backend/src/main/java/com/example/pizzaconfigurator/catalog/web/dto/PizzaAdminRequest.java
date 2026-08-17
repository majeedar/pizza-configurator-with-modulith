package com.example.pizzaconfigurator.catalog.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record PizzaAdminRequest(
    @NotBlank String code,
    @NotBlank String name,
    String description,
    @NotNull @PositiveOrZero BigDecimal basePrice,
    boolean active
) {
}
