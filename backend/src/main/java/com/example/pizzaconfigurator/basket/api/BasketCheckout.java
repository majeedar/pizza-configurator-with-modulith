package com.example.pizzaconfigurator.basket.api;

import java.util.UUID;

/**
 * Published API Order Module uses to consume a basket at checkout time
 * (agent.md §7.6/§7.7). Reading the snapshot never mutates the basket —
 * only {@link #markCheckedOut} does, and only after the caller has
 * successfully created the Order.
 */
public interface BasketCheckout {

    BasketSnapshot getSnapshotForCheckout(UUID basketId);

    void markCheckedOut(UUID basketId);
}
