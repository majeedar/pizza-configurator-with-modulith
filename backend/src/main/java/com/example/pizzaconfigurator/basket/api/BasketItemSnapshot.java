package com.example.pizzaconfigurator.basket.api;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Everything Order Module needs to build an OrderItem (agent.md §5.1
 * OrderItem) — captured immutably at "add to basket" time, not re-derived
 * from the (possibly since-edited) ConfigurationSession.
 */
public record BasketItemSnapshot(
    UUID basketItemId,
    UUID configurationId,
    int quantity,
    UUID pizzaId,
    String pizzaCode,
    String pizzaNameSnapshot,
    String sizeCode,
    String doughCode,
    String modificationsJson,
    String ruleVersion,
    String priceVersion,
    BigDecimal unitPrice,
    String currency
) {
}
