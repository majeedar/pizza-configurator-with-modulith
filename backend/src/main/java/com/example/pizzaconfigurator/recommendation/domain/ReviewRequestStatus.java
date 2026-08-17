package com.example.pizzaconfigurator.recommendation.domain;

/** Agent.md §5.1 {@code ReviewRequest.status}. */
public enum ReviewRequestStatus {
    OPEN,
    ACCEPTED_BY_KITCHEN,
    RECOMMENDED_BY_KITCHEN,
    REJECTED_BY_KITCHEN,
    RECOMMENDATION_ACCEPTED_BY_CUSTOMER,
    RECOMMENDATION_REJECTED_BY_CUSTOMER
}
