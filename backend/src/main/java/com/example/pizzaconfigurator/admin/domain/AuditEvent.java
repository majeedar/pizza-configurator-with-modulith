package com.example.pizzaconfigurator.admin.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Agent.md §14.4/§30: audit of admin rule/price/app-link changes. Never
 * carries passwords, tokens, or other secrets in {@code beforeJson}/{@code
 * afterJson} — callers are responsible for only serializing non-secret
 * fields into those snapshots.
 */
@Entity
@Table(name = "audit_event", schema = "admin")
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID eventId;

    private Instant timestamp;
    private String actorId;
    private String actorRole;
    private String action;
    private String entityType;
    private String entityId;
    private String beforeJson;
    private String afterJson;
    private String correlationId;

    protected AuditEvent() {
    }

    public AuditEvent(
        String actorId, String actorRole, String action, String entityType, String entityId,
        String beforeJson, String afterJson, String correlationId, Clock clock
    ) {
        this.timestamp = Instant.now(clock);
        this.actorId = actorId;
        this.actorRole = actorRole;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.beforeJson = beforeJson;
        this.afterJson = afterJson;
        this.correlationId = correlationId;
    }

    public UUID getEventId() {
        return eventId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getActorId() {
        return actorId;
    }

    public String getActorRole() {
        return actorRole;
    }

    public String getAction() {
        return action;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getBeforeJson() {
        return beforeJson;
    }

    public String getAfterJson() {
        return afterJson;
    }

    public String getCorrelationId() {
        return correlationId;
    }
}
