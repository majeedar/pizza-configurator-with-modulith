package com.example.pizzaconfigurator.orders.api;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Published once an Order and its OrderItems are durably persisted (agent.md
 * §16) — Kitchen (§7.8) and Notification (§7.9) modules react to this
 * asynchronously rather than being called synchronously from checkout.
 */
public record OrderPlaced(
    UUID orderId,
    String displayNumber,
    UUID customerId,
    String fcmDeviceToken,
    BigDecimal totalPrice,
    String currency
) {
}
