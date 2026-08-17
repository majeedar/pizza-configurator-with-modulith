package com.example.pizzaconfigurator.recommendation.api;

import com.example.pizzaconfigurator.recommendation.domain.ReviewRequestStatus;
import java.util.UUID;

/** Agent.md §16: emitted for the Accept/Reject-by-kitchen paths (the Recommend path uses {@link RecommendationCreated} instead). */
public record ConfigurationReviewResolved(UUID reviewRequestId, UUID configurationId, ReviewRequestStatus outcome) {
}
