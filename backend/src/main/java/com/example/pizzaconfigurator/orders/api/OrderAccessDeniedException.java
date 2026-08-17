package com.example.pizzaconfigurator.orders.api;

/**
 * Agent.md §14.3: raised when a status lookup is attempted without a valid
 * guest access token or a matching authenticated-customer session. Never
 * reveals whether the display number itself exists.
 */
public class OrderAccessDeniedException extends RuntimeException {

    public OrderAccessDeniedException() {
        super("Not authorized to access this order");
    }
}
