package com.example.pizzaconfigurator.kitchen.web;

import com.example.pizzaconfigurator.kitchen.application.KitchenSseBroadcaster;
import com.example.pizzaconfigurator.orders.api.OrderQuery;
import com.example.pizzaconfigurator.orders.api.OrderTransitions;
import com.example.pizzaconfigurator.orders.api.OrderView;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Agent.md §7.8/§9.2 — production board data + commands. Every endpoint
 * here requires {@code ROLE_KITCHEN} or {@code ROLE_ADMIN}, enforced by the
 * Spring Security filter chain in the {@code security} module (path-based
 * on {@code /api/v1/kitchen/**}), not by this controller. The review-queue
 * endpoints from §9.2 (`/kitchen/reviews/**`) belong to the not-yet-built
 * {@code recommendation} module (Phase 10) and are intentionally absent.
 */
@RestController
@RequestMapping("/api/v1/kitchen")
class KitchenController {

    private final OrderQuery orderQuery;
    private final OrderTransitions orderTransitions;
    private final KitchenSseBroadcaster broadcaster;

    KitchenController(OrderQuery orderQuery, OrderTransitions orderTransitions, KitchenSseBroadcaster broadcaster) {
        this.orderQuery = orderQuery;
        this.orderTransitions = orderTransitions;
        this.broadcaster = broadcaster;
    }

    @GetMapping("/orders")
    List<OrderView> activeOrders() {
        return orderQuery.findActiveOrders();
    }

    @PostMapping("/orders/{orderId}/approve")
    OrderView approve(@PathVariable UUID orderId) {
        return orderTransitions.approve(orderId);
    }

    @PostMapping("/orders/{orderId}/start")
    OrderView start(@PathVariable UUID orderId) {
        return orderTransitions.start(orderId);
    }

    @PostMapping("/orders/{orderId}/ready")
    OrderView ready(@PathVariable UUID orderId) {
        return orderTransitions.markReady(orderId);
    }

    @PostMapping("/orders/{orderId}/complete")
    OrderView complete(@PathVariable UUID orderId) {
        return orderTransitions.complete(orderId);
    }

    @GetMapping("/stream")
    SseEmitter stream() {
        return broadcaster.subscribe();
    }
}
