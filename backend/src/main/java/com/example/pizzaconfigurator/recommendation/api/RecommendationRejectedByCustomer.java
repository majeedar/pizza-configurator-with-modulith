package com.example.pizzaconfigurator.recommendation.api;

import java.util.UUID;

public record RecommendationRejectedByCustomer(UUID reviewRequestId, UUID configurationId) {
}
