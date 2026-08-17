package com.example.pizzaconfigurator.catalog.api;

import java.math.BigDecimal;

public record SizeOptionView(
    String code,
    String displayName,
    BigDecimal priceModifier
) {
}
