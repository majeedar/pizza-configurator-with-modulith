package com.example.pizzaconfigurator.orders.domain;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Agent.md §7.7: {@code CONFIRMED → APPROVED → IN_PROCESSING → READY →
 * COMPLETED} is the happy path. "Do not allow invalid reverse transitions
 * without an explicit administrative recovery mechanism" — no such recovery
 * mechanism exists yet, so this table currently has no reverse edges at
 * all; every non-terminal state can also move to CANCELLED, and only
 * CONFIRMED/APPROVED can be REJECTED (once kitchen has started processing,
 * "reject" no longer makes sense — that's a cancellation instead).
 */
public enum OrderStatus {
    CONFIRMED,
    APPROVED,
    IN_PROCESSING,
    READY,
    COMPLETED,
    CANCELLED,
    REJECTED;

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(OrderStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(CONFIRMED, EnumSet.of(APPROVED, CANCELLED, REJECTED));
        ALLOWED_TRANSITIONS.put(APPROVED, EnumSet.of(IN_PROCESSING, CANCELLED, REJECTED));
        ALLOWED_TRANSITIONS.put(IN_PROCESSING, EnumSet.of(READY, CANCELLED));
        ALLOWED_TRANSITIONS.put(READY, EnumSet.of(COMPLETED));
        ALLOWED_TRANSITIONS.put(COMPLETED, EnumSet.noneOf(OrderStatus.class));
        ALLOWED_TRANSITIONS.put(CANCELLED, EnumSet.noneOf(OrderStatus.class));
        ALLOWED_TRANSITIONS.put(REJECTED, EnumSet.noneOf(OrderStatus.class));
    }

    public boolean canTransitionTo(OrderStatus next) {
        return ALLOWED_TRANSITIONS.get(this).contains(next);
    }
}
