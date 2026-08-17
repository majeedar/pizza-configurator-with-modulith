package com.example.pizzaconfigurator.recommendation.api;

import java.util.UUID;

public record RecommendationAcceptedByCustomer(UUID reviewRequestId, UUID configurationId) {
}
