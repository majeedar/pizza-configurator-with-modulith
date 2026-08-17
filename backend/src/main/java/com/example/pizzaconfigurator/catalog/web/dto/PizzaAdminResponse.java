package com.example.pizzaconfigurator.catalog.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PizzaAdminResponse(
    UUID pizzaId,
    String code,
    String name,
    String description,
    BigDecimal basePrice,
    boolean active,
    Long version
) {
}
