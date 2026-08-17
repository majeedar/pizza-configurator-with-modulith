package com.example.pizzaconfigurator.catalog.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record DoughAdminRequest(
    @NotBlank String code,
    @NotBlank String displayName,
    @NotNull BigDecimal priceModifier,
    boolean active
) {
}
