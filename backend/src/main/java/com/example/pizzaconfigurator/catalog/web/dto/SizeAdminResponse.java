package com.example.pizzaconfigurator.catalog.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SizeAdminResponse(
    UUID sizeId,
    String code,
    String displayName,
    BigDecimal priceModifier,
    boolean active
) {
}
