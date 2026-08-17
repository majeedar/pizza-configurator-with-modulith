package com.example.pizzaconfigurator.basket.api;

import java.util.UUID;

public class BasketAlreadyCheckedOutException extends RuntimeException {

    public BasketAlreadyCheckedOutException(UUID basketId) {
        super("Basket " + basketId + " has already been checked out");
    }
}
