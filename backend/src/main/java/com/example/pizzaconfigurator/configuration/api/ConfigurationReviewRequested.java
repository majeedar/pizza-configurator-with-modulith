package com.example.pizzaconfigurator.configuration.api;

import java.util.UUID;

/**
 * Agent.md §4.3 step 3 / §16: published when the Configuration Module
 * cannot resolve a request automatically — either the Rule Module returned
 * {@code INVALID} with no deterministic suggestion, or the AI Adapter
 * could not fully interpret a comment (agent.md §13.3). The
 * {@code recommendation} module (§7.11) listens for this to create the
 * {@code ReviewRequest}; Configuration Module knows nothing about
 * {@code ReviewRequest} itself, avoiding a dependency cycle (recommendation
 * already depends on configuration to revalidate/reprice).
 */
public record ConfigurationReviewRequested(UUID configurationId, String reason, String originalRequestJson) {
}
