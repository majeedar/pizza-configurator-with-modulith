package com.example.pizzaconfigurator.pricing.api;

import com.example.pizzaconfigurator.pricing.domain.ItemType;

public class PriceNotDefinedException extends RuntimeException {

    public PriceNotDefinedException(ItemType itemType, String itemId) {
        super("No active price defined for " + itemType + " '" + itemId + "'");
    }
}
