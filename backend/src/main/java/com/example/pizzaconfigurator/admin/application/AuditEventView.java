package com.example.pizzaconfigurator.admin.application;

import java.time.Instant;
import java.util.UUID;

public record AuditEventView(
    UUID eventId,
    Instant timestamp,
    String actorId,
    String actorRole,
    String action,
    String entityType,
    String entityId,
    String beforeJson,
    String afterJson,
    String correlationId
) {
}
