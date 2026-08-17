package com.example.pizzaconfigurator.catalog.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PizzaView(
    UUID pizzaId,
    String code,
    String name,
    String description,
    BigDecimal basePrice,
    String imageUrl,
    List<RecipeItemView> recipe
) {
}
