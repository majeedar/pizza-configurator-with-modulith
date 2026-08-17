package com.example.pizzaconfigurator.orders.application;

import com.example.pizzaconfigurator.orders.infrastructure.persistence.OrderRepository;
import org.springframework.stereotype.Component;

/**
 * Human-readable order number (agent.md §5.1) — not the technical
 * {@code orderId}. Backed by the {@code orders.display_number_seq} Postgres
 * sequence so concurrent checkouts never collide.
 */
@Component
class DisplayNumberGenerator {

    private final OrderRepository orders;

    DisplayNumberGenerator(OrderRepository orders) {
        this.orders = orders;
    }

    String next() {
        long sequenceValue = orders.nextDisplayNumberSequenceValue();
        return "P-%05d".formatted(sequenceValue);
    }
}
