package com.example.pizzaconfigurator.basket.api;

import java.util.UUID;

public class BasketNotFoundException extends RuntimeException {

    public BasketNotFoundException(UUID basketId) {
        super("No basket found for id " + basketId);
    }
}
