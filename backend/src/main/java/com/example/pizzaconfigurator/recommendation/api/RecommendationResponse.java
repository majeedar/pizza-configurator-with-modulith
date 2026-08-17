package com.example.pizzaconfigurator.recommendation.api;

import java.util.UUID;

/** Agent.md §7.11: the customer's response to a {@code RECOMMENDED_BY_KITCHEN} review request. */
public interface RecommendationResponse {

    ReviewOutcome acceptRecommendation(UUID reviewRequestId);

    ReviewOutcome rejectRecommendation(UUID reviewRequestId);
}
