package com.example.pizzaconfigurator.recommendation.api;

import com.example.pizzaconfigurator.recommendation.domain.CustomerResponse;
import com.example.pizzaconfigurator.recommendation.domain.ReviewRequestStatus;
import java.time.Instant;
import java.util.UUID;

public record ReviewRequestView(
    UUID reviewRequestId,
    UUID configurationId,
    ReviewRequestStatus status,
    String reason,
    String originalRequestJson,
    String proposedModificationJson,
    String reviewedBy,
    Instant reviewedAt,
    CustomerResponse customerResponse,
    Instant customerRespondedAt,
    Instant createdAt
) {
}
