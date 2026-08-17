package com.example.pizzaconfigurator.orders.api;

import com.example.pizzaconfigurator.orders.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderView(
    UUID orderId,
    String displayNumber,
    OrderStatus status,
    BigDecimal totalPrice,
    String currency,
    UUID customerId,
    String customNotes,
    String pickupToken,
    List<OrderItemView> items,
    Instant createdAt
) {
}
