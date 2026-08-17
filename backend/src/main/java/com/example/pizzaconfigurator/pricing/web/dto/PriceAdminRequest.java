package com.example.pizzaconfigurator.pricing.web.dto;

import com.example.pizzaconfigurator.pricing.domain.ItemType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.Instant;

public record PriceAdminRequest(
    @NotNull ItemType itemType,
    @NotBlank String itemId,
    @NotNull @PositiveOrZero BigDecimal amount,
    @NotBlank String currency,
    boolean active,
    Instant validFrom,
    Instant validTo
) {
}
