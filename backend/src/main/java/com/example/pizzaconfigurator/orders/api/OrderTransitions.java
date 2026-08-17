package com.example.pizzaconfigurator.orders.api;

import java.util.UUID;

/**
 * Published API Kitchen Module (§7.8) commands drive (agent.md §9.2
 * {@code POST /api/v1/kitchen/orders/{orderId}/approve|start|ready|complete}).
 * Kitchen must not edit price/rule definitions — this interface only
 * exposes state transitions, nothing else.
 */
public interface OrderTransitions {

    OrderView approve(UUID orderId);

    OrderView start(UUID orderId);

    OrderView markReady(UUID orderId);

    OrderView complete(UUID orderId);
}
