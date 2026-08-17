package com.example.pizzaconfigurator.recommendation.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Agent.md §5.1/§7.11: the kitchen-side triage of an unresolved/invalid
 * configuration, and the customer's response to a kitchen recommendation.
 * Regardless of path, this never creates an Order itself (agent.md §4.3).
 */
@Entity
@Table(name = "review_request", schema = "recommendation")
@EntityListeners(AuditingEntityListener.class)
public class ReviewRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID reviewRequestId;

    private UUID configurationId;

    @Enumerated(EnumType.STRING)
    private ReviewRequestStatus status;

    private String reason;
    private String originalRequestJson;
    private String proposedModificationJson;
    private String reviewedBy;
    private Instant reviewedAt;

    @Enumerated(EnumType.STRING)
    private CustomerResponse customerResponse;

    private Instant customerRespondedAt;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    protected ReviewRequest() {
    }

    public ReviewRequest(UUID configurationId, String reason, String originalRequestJson) {
        this.configurationId = configurationId;
        this.status = ReviewRequestStatus.OPEN;
        this.reason = reason;
        this.originalRequestJson = originalRequestJson;
    }

    public void acceptByKitchen(String reviewedBy, Clock clock) {
        requireStatus(ReviewRequestStatus.OPEN, "accept");
        this.status = ReviewRequestStatus.ACCEPTED_BY_KITCHEN;
        this.reviewedBy = reviewedBy;
        this.reviewedAt = Instant.now(clock);
    }

    public void recommendByKitchen(String reviewedBy, String proposedModificationJson, Clock clock) {
        requireStatus(ReviewRequestStatus.OPEN, "recommend on");
        this.status = ReviewRequestStatus.RECOMMENDED_BY_KITCHEN;
        this.reviewedBy = reviewedBy;
        this.reviewedAt = Instant.now(clock);
        this.proposedModificationJson = proposedModificationJson;
    }

    public void rejectByKitchen(String reviewedBy, String reason, Clock clock) {
        requireStatus(ReviewRequestStatus.OPEN, "reject");
        this.status = ReviewRequestStatus.REJECTED_BY_KITCHEN;
        this.reviewedBy = reviewedBy;
        this.reviewedAt = Instant.now(clock);
        if (reason != null && !reason.isBlank()) {
            this.reason = reason;
        }
    }

    public void acceptByCustomer(Clock clock) {
        requireStatus(ReviewRequestStatus.RECOMMENDED_BY_KITCHEN, "accept");
        this.status = ReviewRequestStatus.RECOMMENDATION_ACCEPTED_BY_CUSTOMER;
        this.customerResponse = CustomerResponse.ACCEPTED;
        this.customerRespondedAt = Instant.now(clock);
    }

    public void rejectByCustomer(Clock clock) {
        requireStatus(ReviewRequestStatus.RECOMMENDED_BY_KITCHEN, "reject");
        this.status = ReviewRequestStatus.RECOMMENDATION_REJECTED_BY_CUSTOMER;
        this.customerResponse = CustomerResponse.REJECTED;
        this.customerRespondedAt = Instant.now(clock);
    }

    private void requireStatus(ReviewRequestStatus required, String action) {
        if (status != required) {
            throw new IllegalReviewTransitionException(status, action);
        }
    }

    public UUID getReviewRequestId() {
        return reviewRequestId;
    }

    public UUID getConfigurationId() {
        return configurationId;
    }

    public ReviewRequestStatus getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public String getOriginalRequestJson() {
        return originalRequestJson;
    }

    public String getProposedModificationJson() {
        return proposedModificationJson;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public CustomerResponse getCustomerResponse() {
        return customerResponse;
    }

    public Instant getCustomerRespondedAt() {
        return customerRespondedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
