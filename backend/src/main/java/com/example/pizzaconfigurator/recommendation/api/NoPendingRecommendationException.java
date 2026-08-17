package com.example.pizzaconfigurator.recommendation.api;

import java.util.UUID;

/** Agent.md §9.1: raised when a customer polls/responds to a recommendation that doesn't exist (yet, or anymore). */
public class NoPendingRecommendationException extends RuntimeException {

    public NoPendingRecommendationException(UUID configurationId) {
        super("No pending kitchen recommendation for configuration " + configurationId);
    }
}
