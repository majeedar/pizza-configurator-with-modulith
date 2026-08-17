package com.example.pizzaconfigurator.recommendation.api;

import java.util.UUID;

/** Wraps the domain-layer transition guard so callers never need {@code recommendation.domain}. */
public class IllegalReviewStateException extends RuntimeException {

    public IllegalReviewStateException(UUID reviewRequestId, String message) {
        super("Review request " + reviewRequestId + ": " + message);
    }
}
