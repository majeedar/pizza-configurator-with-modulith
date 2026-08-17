package com.example.pizzaconfigurator.orders.api;

import java.util.List;
import java.util.UUID;

public interface OrderQuery {

    OrderView getOrder(UUID orderId);

    OrderView getOrderByDisplayNumber(String displayNumber);

    /**
     * Orders not yet in a terminal state — the queue a kitchen/KDS view
     * consumes (agent.md §7.8, Phase 6 DoD "appears in kitchen query").
     */
    List<OrderView> findActiveOrders();
}
