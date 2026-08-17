package com.example.pizzaconfigurator.recommendation.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecommendationQuery {

    ReviewRequestView getReviewRequest(UUID reviewRequestId);

    /** The kitchen review queue (agent.md §8.2) — everything not yet in a terminal state. */
    List<ReviewRequestView> findQueue();

    Optional<ReviewRequestView> findLatestForConfiguration(UUID configurationId);
}
