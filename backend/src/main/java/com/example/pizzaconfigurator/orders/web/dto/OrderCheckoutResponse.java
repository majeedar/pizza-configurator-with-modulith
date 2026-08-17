package com.example.pizzaconfigurator.orders.web.dto;

import com.example.pizzaconfigurator.orders.api.OrderView;

/**
 * {@code accessToken} is the raw guest order-access token (agent.md §14.3)
 * — present only on the response that actually created the order, and only
 * for guest (no customer session) checkouts. It is never recoverable again
 * afterwards, so the customer web/native app must persist it locally.
 */
public record OrderCheckoutResponse(OrderView order, String accessToken) {
}
