package com.example.pizzaconfigurator.basket.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record BasketSnapshot(
    UUID basketId,
    List<BasketItemSnapshot> items,
    BigDecimal total,
    String currency
) {
}
