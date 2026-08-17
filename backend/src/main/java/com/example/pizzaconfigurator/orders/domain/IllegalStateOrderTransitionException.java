package com.example.pizzaconfigurator.orders.domain;

public class IllegalStateOrderTransitionException extends RuntimeException {

    public IllegalStateOrderTransitionException(OrderStatus from, OrderStatus to) {
        super("Cannot transition order from " + from + " to " + to);
    }
}
