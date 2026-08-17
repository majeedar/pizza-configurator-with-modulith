package com.example.pizzaconfigurator.orders.api;

import java.util.UUID;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(UUID orderId) {
        super("No order found for id " + orderId);
    }

    public OrderNotFoundException(String displayNumber) {
        super("No order found for display number " + displayNumber);
    }
}
