package com.example.pizzaconfigurator.basket.application;

import java.math.BigDecimal;
import java.util.UUID;

public record BasketItemView(
    UUID basketItemId,
    UUID configurationId,
    int quantity,
    String pizzaName,
    String sizeCode,
    String doughCode,
    BigDecimal unitPrice,
    String currency,
    BigDecimal lineTotal
) {
}
