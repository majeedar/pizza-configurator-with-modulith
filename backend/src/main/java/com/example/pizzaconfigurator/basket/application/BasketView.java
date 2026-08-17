package com.example.pizzaconfigurator.basket.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record BasketView(
    UUID basketId,
    String sessionToken,
    List<BasketItemView> items,
    BigDecimal total,
    String currency
) {
}
