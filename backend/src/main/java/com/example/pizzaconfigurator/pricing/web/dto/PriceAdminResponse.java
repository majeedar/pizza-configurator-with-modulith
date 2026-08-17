package com.example.pizzaconfigurator.pricing.web.dto;

import com.example.pizzaconfigurator.pricing.domain.ItemType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PriceAdminResponse(
    UUID priceId,
    ItemType itemType,
    String itemId,
    BigDecimal amount,
    String currency,
    boolean active,
    Long version,
    Instant validFrom,
    Instant validTo
) {
}
