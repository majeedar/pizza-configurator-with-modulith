package com.example.pizzaconfigurator.basket.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record AddBasketItemRequest(@NotNull UUID configurationId, @Positive int quantity) {
}
