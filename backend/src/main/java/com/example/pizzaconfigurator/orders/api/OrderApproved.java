package com.example.pizzaconfigurator.orders.api;

import java.util.UUID;

public record OrderApproved(UUID orderId, String displayNumber, UUID customerId, String fcmDeviceToken) {
}
