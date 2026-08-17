package com.example.pizzaconfigurator.catalog.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record DoughAdminResponse(
    UUID doughId,
    String code,
    String displayName,
    BigDecimal priceModifier,
    boolean active
) {
}
