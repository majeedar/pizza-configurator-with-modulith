package com.example.pizzaconfigurator.pricing.application;

import java.util.UUID;

public class PriceAdminNotFoundException extends RuntimeException {

    public PriceAdminNotFoundException(UUID priceId) {
        super("No price definition found for id " + priceId);
    }
}
