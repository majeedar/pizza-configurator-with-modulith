package com.example.pizzaconfigurator.orders.api;

import java.util.UUID;

public record OrderProcessingStarted(UUID orderId, String displayNumber, UUID customerId, String fcmDeviceToken) {
}
