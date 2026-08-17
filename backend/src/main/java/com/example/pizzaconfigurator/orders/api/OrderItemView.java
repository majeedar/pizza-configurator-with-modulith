package com.example.pizzaconfigurator.orders.api;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemView(
    UUID orderItemId,
    UUID pizzaId,
    String pizzaNameSnapshot,
    String sizeCode,
    String doughCode,
    int quantity,
    String modificationsJson,
    BigDecimal unitPrice,
    BigDecimal subtotal
) {
}
