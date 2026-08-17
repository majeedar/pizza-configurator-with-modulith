package com.example.pizzaconfigurator.orders.application;

import com.example.pizzaconfigurator.orders.api.OrderView;

/**
 * {@code rawAccessToken} is only non-null on the request that actually
 * created the order (guest orders only) — it is never recoverable
 * afterwards, since only its hash is persisted (agent.md §14.3). A replayed
 * idempotent request (§15.1) returns the same {@link OrderView} with a null
 * token.
 */
public record OrderCheckoutResult(OrderView order, String rawAccessToken) {
}
