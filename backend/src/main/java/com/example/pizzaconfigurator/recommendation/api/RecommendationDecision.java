package com.example.pizzaconfigurator.recommendation.api;

import com.example.pizzaconfigurator.configuration.api.ConfigurationPatch;
import java.util.UUID;

/** Agent.md §7.11: the Kitchen decision API on an {@code OPEN} review request. */
public interface RecommendationDecision {

    ReviewOutcome accept(UUID reviewRequestId, String reviewedBy);

    ReviewOutcome recommend(UUID reviewRequestId, String reviewedBy, ConfigurationPatch patch);

    ReviewOutcome reject(UUID reviewRequestId, String reviewedBy, String reason);
}
