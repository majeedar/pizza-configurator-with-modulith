package com.example.pizzaconfigurator.recommendation.domain;

public class IllegalReviewTransitionException extends RuntimeException {

    public IllegalReviewTransitionException(ReviewRequestStatus from, String action) {
        super("Cannot " + action + " a review request in status " + from);
    }
}
