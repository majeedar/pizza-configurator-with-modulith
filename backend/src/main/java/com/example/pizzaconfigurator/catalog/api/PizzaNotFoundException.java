package com.example.pizzaconfigurator.catalog.api;

import java.util.UUID;

public class PizzaNotFoundException extends RuntimeException {

    public PizzaNotFoundException(UUID pizzaId) {
        super("No pizza found for id " + pizzaId);
    }
}
