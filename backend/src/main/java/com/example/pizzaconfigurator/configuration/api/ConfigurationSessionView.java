package com.example.pizzaconfigurator.configuration.api;

import com.example.pizzaconfigurator.configuration.domain.PriceStatus;
import com.example.pizzaconfigurator.configuration.domain.ValidationStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record ConfigurationSessionView(
    UUID configurationId,
    UUID customerId,
    UUID pizzaId,
    String sizeCode,
    String doughCode,
    Set<String> removedIngredientCodes,
    List<SelectionExtra> extras,
    String comment,
    ValidationStatus validationStatus,
    String ruleVersion,
    PriceStatus priceStatus,
    BigDecimal calculatedPrice,
    String priceVersion,
    String currency,
    Instant expiresAt
) {
    public boolean isReadyForCheckout() {
        boolean validationOk = validationStatus == ValidationStatus.VALID || validationStatus == ValidationStatus.REVIEW_APPROVED;
        return validationOk && priceStatus == PriceStatus.READY_FOR_CHECKOUT;
    }
}
