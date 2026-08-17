package com.example.pizzaconfigurator.recommendation.api;

import java.util.UUID;

public class ReviewRequestNotFoundException extends RuntimeException {

    public ReviewRequestNotFoundException(UUID reviewRequestId) {
        super("No review request found for id " + reviewRequestId);
    }
}
