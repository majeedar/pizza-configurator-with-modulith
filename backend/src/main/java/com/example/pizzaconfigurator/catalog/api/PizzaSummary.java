package com.example.pizzaconfigurator.catalog.api;

import java.math.BigDecimal;
import java.util.UUID;

public record PizzaSummary(
    UUID pizzaId,
    String code,
    String name,
    String description,
    BigDecimal basePrice
) {
}
