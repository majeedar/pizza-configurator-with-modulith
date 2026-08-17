package com.example.pizzaconfigurator.orders.api;

import java.util.UUID;

/**
 * Agent.md §7.9: the Notification Module reacts to this to send the
 * ready/pickup notification (email + push).
 */
public record OrderReady(UUID orderId, String displayNumber, UUID customerId, String fcmDeviceToken) {
}
