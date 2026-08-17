package com.example.pizzaconfigurator.orders.api;

import com.example.pizzaconfigurator.orders.domain.OrderStatus;
import java.util.UUID;

/**
 * Agent.md §18 {@code ORDER_STATE_CONFLICT} — a Kitchen command was
 * attempted against an order not in the right state (e.g. "ready" on an
 * order that's still CONFIRMED). Wraps the domain-layer
 * {@code IllegalStateOrderTransitionException} so callers outside the
 * {@code orders} module never need to depend on {@code orders.domain}.
 */
public class OrderStateConflictException extends RuntimeException {

    public OrderStateConflictException(UUID orderId, OrderStatus from, OrderStatus to) {
        super("Order " + orderId + " cannot move from " + from + " to " + to);
    }
}
