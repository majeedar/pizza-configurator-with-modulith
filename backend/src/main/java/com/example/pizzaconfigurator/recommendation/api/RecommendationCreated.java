package com.example.pizzaconfigurator.recommendation.api;

import java.util.UUID;

/** Agent.md §16: kitchen proposed an alternative configuration — the customer must explicitly Accept or Reject it. */
public record RecommendationCreated(UUID reviewRequestId, UUID configurationId) {
}
