package com.example.pizzaconfigurator.admin.api;

/**
 * Agent.md §14.4/§30: what a caller reports to the audit log after an
 * admin change. {@code beforeJson}/{@code afterJson} are nullable and must
 * never contain passwords, tokens, or other secrets — that's the caller's
 * responsibility, not this module's, since only the caller knows which
 * fields are sensitive.
 */
public record AuditEntry(
    String actorId,
    String actorRole,
    String action,
    String entityType,
    String entityId,
    String beforeJson,
    String afterJson
) {
}
