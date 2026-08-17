package com.example.pizzaconfigurator.orders.application;

/**
 * Agent.md §15.1: the same {@code Idempotency-Key} was reused with a
 * different request body.
 */
public class IdempotencyKeyConflictException extends RuntimeException {

    public IdempotencyKeyConflictException(String idempotencyKey) {
        super("Idempotency-Key " + idempotencyKey + " was already used with a different request");
    }
}
