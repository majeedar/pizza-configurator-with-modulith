package com.example.pizzaconfigurator.catalog.application;

public class InvalidPizzaImageException extends RuntimeException {

    public InvalidPizzaImageException(String message) {
        super(message);
    }
}
