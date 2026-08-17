package com.example.pizzaconfigurator.basket.api;

import java.util.UUID;

public class BasketEmptyException extends RuntimeException {

    public BasketEmptyException(UUID basketId) {
        super("Basket " + basketId + " has no items — nothing to check out");
    }
}
