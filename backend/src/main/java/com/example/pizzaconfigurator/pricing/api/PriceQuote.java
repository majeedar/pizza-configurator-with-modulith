package com.example.pizzaconfigurator.pricing.api;

import java.math.BigDecimal;

public record PriceQuote(
    String currency,
    BigDecimal base,
    BigDecimal size,
    BigDecimal dough,
    BigDecimal extras,
    BigDecimal total,
    String priceVersion
) {
}
