package com.example.pizzaconfigurator.orders.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateOrderRequest(
    @NotNull UUID basketId,
    @Size(max = 1000) String customNotes,
    String fcmDeviceToken
) {
}
