package com.example.pizzaconfigurator.kitchen.application;

import com.example.pizzaconfigurator.orders.api.OrderApproved;
import com.example.pizzaconfigurator.orders.api.OrderCompleted;
import com.example.pizzaconfigurator.orders.api.OrderPlaced;
import com.example.pizzaconfigurator.orders.api.OrderProcessingStarted;
import com.example.pizzaconfigurator.orders.api.OrderReady;
import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Agent.md §17: SSE from backend to KDS for server-to-client order updates
 * — an optimization, not the only source of truth (clients refetch
 * {@code GET /api/v1/kitchen/orders} on (re)connect). Listens
 * {@link TransactionPhase#AFTER_COMMIT} so the KDS is only ever told about
 * an order once it's actually durably persisted, never one that might
 * still roll back.
 */
@Component
public class KitchenSseBroadcaster {

    private static final Long NO_TIMEOUT = 0L;

    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(NO_TIMEOUT);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(exception -> emitters.remove(emitter));
        emitters.add(emitter);
        return emitter;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onOrderPlaced(OrderPlaced event) {
        broadcast("order.created", event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onOrderApproved(OrderApproved event) {
        broadcast("order.updated", event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onOrderProcessingStarted(OrderProcessingStarted event) {
        broadcast("order.updated", event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onOrderReady(OrderReady event) {
        broadcast("order.ready", event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onOrderCompleted(OrderCompleted event) {
        broadcast("order.updated", event);
    }

    private void broadcast(String eventName, Object payload) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(payload));
            } catch (IOException | IllegalStateException e) {
                emitters.remove(emitter);
            }
        }
    }
}
