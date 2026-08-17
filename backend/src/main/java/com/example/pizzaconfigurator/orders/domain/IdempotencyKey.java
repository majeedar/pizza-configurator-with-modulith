package com.example.pizzaconfigurator.orders.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Agent.md §15.1: a repeated identical request (same key, same request hash)
 * returns the previously created Order; a repeated key with a different
 * request is a conflict.
 */
@Entity
@Table(name = "idempotency_key", schema = "orders")
@EntityListeners(AuditingEntityListener.class)
public class IdempotencyKey {

    @Id
    private String idempotencyKey;

    private String requestHash;
    private UUID orderId;

    @CreatedDate
    private Instant createdAt;

    protected IdempotencyKey() {
    }

    public IdempotencyKey(String idempotencyKey, String requestHash, UUID orderId) {
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
        this.orderId = orderId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public UUID getOrderId() {
        return orderId;
    }
}
