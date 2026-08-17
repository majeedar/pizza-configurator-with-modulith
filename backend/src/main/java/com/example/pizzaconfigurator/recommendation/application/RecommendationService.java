package com.example.pizzaconfigurator.recommendation.application;

import com.example.pizzaconfigurator.configuration.api.ConfigurationPatch;
import com.example.pizzaconfigurator.configuration.api.ConfigurationQuery;
import com.example.pizzaconfigurator.configuration.api.ConfigurationReviewIntegration;
import com.example.pizzaconfigurator.configuration.api.ConfigurationReviewRequested;
import com.example.pizzaconfigurator.configuration.api.ConfigurationSessionView;
import com.example.pizzaconfigurator.recommendation.api.ConfigurationReviewResolved;
import com.example.pizzaconfigurator.recommendation.api.IllegalReviewStateException;
import com.example.pizzaconfigurator.recommendation.api.RecommendationAcceptedByCustomer;
import com.example.pizzaconfigurator.recommendation.api.RecommendationCreated;
import com.example.pizzaconfigurator.recommendation.api.RecommendationDecision;
import com.example.pizzaconfigurator.recommendation.api.RecommendationQuery;
import com.example.pizzaconfigurator.recommendation.api.RecommendationRejectedByCustomer;
import com.example.pizzaconfigurator.recommendation.api.RecommendationResponse;
import com.example.pizzaconfigurator.recommendation.api.ReviewOutcome;
import com.example.pizzaconfigurator.recommendation.api.ReviewRequestNotFoundException;
import com.example.pizzaconfigurator.recommendation.api.ReviewRequestView;
import com.example.pizzaconfigurator.recommendation.domain.IllegalReviewTransitionException;
import com.example.pizzaconfigurator.recommendation.domain.ReviewRequest;
import com.example.pizzaconfigurator.recommendation.domain.ReviewRequestStatus;
import com.example.pizzaconfigurator.recommendation.infrastructure.persistence.ReviewRequestRepository;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.json.JsonMapper;

/**
 * Agent.md §7.11: owns the kitchen-side triage of a {@link ReviewRequest}
 * and the customer's response to a kitchen recommendation. Never creates
 * an Order itself (agent.md §4.3) — every path here only ever revalidates
 * and reprices a {@code ConfigurationSession} through {@link
 * ConfigurationReviewIntegration}.
 */
@Service
@Transactional
public class RecommendationService implements RecommendationDecision, RecommendationResponse, RecommendationQuery {

    private static final List<ReviewRequestStatus> QUEUE_STATUSES =
        List.of(ReviewRequestStatus.OPEN, ReviewRequestStatus.RECOMMENDED_BY_KITCHEN);

    private final ReviewRequestRepository reviewRequests;
    private final ConfigurationReviewIntegration configurationReviewIntegration;
    private final ConfigurationQuery configurationQuery;
    private final ApplicationEventPublisher events;
    private final JsonMapper jsonMapper;
    private final Clock clock;

    RecommendationService(
        ReviewRequestRepository reviewRequests,
        ConfigurationReviewIntegration configurationReviewIntegration,
        ConfigurationQuery configurationQuery,
        ApplicationEventPublisher events,
        JsonMapper jsonMapper,
        Clock clock
    ) {
        this.reviewRequests = reviewRequests;
        this.configurationReviewIntegration = configurationReviewIntegration;
        this.configurationQuery = configurationQuery;
        this.events = events;
        this.jsonMapper = jsonMapper;
        this.clock = clock;
    }

    /**
     * Agent.md §4.3 step 3/4: creates the {@code ReviewRequest} and puts it
     * on the kitchen queue. Runs in its own fresh transaction — an {@code
     * AFTER_COMMIT} listener calling a default-propagation repository save
     * can silently fail to persist, since the triggering transaction's
     * synchronization state hasn't finished tearing down yet (see the
     * identical issue and fix in {@code NotificationService}, Phase 8).
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void onConfigurationReviewRequested(ConfigurationReviewRequested event) {
        reviewRequests.save(new ReviewRequest(event.configurationId(), event.reason(), event.originalRequestJson()));
    }

    @Override
    public ReviewOutcome accept(UUID reviewRequestId, String reviewedBy) {
        ReviewRequest review = getEntity(reviewRequestId);
        transition(review, () -> review.acceptByKitchen(reviewedBy, clock));
        ConfigurationSessionView session = configurationReviewIntegration.approveOriginal(review.getConfigurationId());
        events.publishEvent(new ConfigurationReviewResolved(review.getReviewRequestId(), review.getConfigurationId(), review.getStatus()));
        return new ReviewOutcome(toView(review), session);
    }

    @Override
    public ReviewOutcome recommend(UUID reviewRequestId, String reviewedBy, ConfigurationPatch patch) {
        ReviewRequest review = getEntity(reviewRequestId);
        String patchJson = jsonMapper.writeValueAsString(patch);
        transition(review, () -> review.recommendByKitchen(reviewedBy, patchJson, clock));
        events.publishEvent(new RecommendationCreated(review.getReviewRequestId(), review.getConfigurationId()));
        return new ReviewOutcome(toView(review), configurationQuery.getSession(review.getConfigurationId()));
    }

    @Override
    public ReviewOutcome reject(UUID reviewRequestId, String reviewedBy, String reason) {
        ReviewRequest review = getEntity(reviewRequestId);
        transition(review, () -> review.rejectByKitchen(reviewedBy, reason, clock));
        ConfigurationSessionView session = configurationReviewIntegration.reject(review.getConfigurationId());
        events.publishEvent(new ConfigurationReviewResolved(review.getReviewRequestId(), review.getConfigurationId(), review.getStatus()));
        return new ReviewOutcome(toView(review), session);
    }

    @Override
    public ReviewOutcome acceptRecommendation(UUID reviewRequestId) {
        ReviewRequest review = getEntity(reviewRequestId);
        transition(review, () -> review.acceptByCustomer(clock));
        ConfigurationPatch patch = jsonMapper.readValue(review.getProposedModificationJson(), ConfigurationPatch.class);
        ConfigurationSessionView session = configurationReviewIntegration.approveWithPatch(review.getConfigurationId(), patch);
        events.publishEvent(new RecommendationAcceptedByCustomer(review.getReviewRequestId(), review.getConfigurationId()));
        return new ReviewOutcome(toView(review), session);
    }

    @Override
    public ReviewOutcome rejectRecommendation(UUID reviewRequestId) {
        ReviewRequest review = getEntity(reviewRequestId);
        transition(review, () -> review.rejectByCustomer(clock));
        ConfigurationSessionView session = configurationReviewIntegration.reject(review.getConfigurationId());
        events.publishEvent(new RecommendationRejectedByCustomer(review.getReviewRequestId(), review.getConfigurationId()));
        return new ReviewOutcome(toView(review), session);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewRequestView getReviewRequest(UUID reviewRequestId) {
        return toView(getEntity(reviewRequestId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewRequestView> findQueue() {
        return reviewRequests.findByStatusInOrderByCreatedAtAsc(QUEUE_STATUSES).stream().map(this::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReviewRequestView> findLatestForConfiguration(UUID configurationId) {
        return reviewRequests.findFirstByConfigurationIdOrderByCreatedAtDesc(configurationId).map(this::toView);
    }

    private void transition(ReviewRequest review, Runnable transition) {
        try {
            transition.run();
        } catch (IllegalReviewTransitionException e) {
            throw new IllegalReviewStateException(review.getReviewRequestId(), e.getMessage());
        }
    }

    private ReviewRequest getEntity(UUID reviewRequestId) {
        return reviewRequests.findById(reviewRequestId).orElseThrow(() -> new ReviewRequestNotFoundException(reviewRequestId));
    }

    private ReviewRequestView toView(ReviewRequest review) {
        return new ReviewRequestView(
            review.getReviewRequestId(), review.getConfigurationId(), review.getStatus(), review.getReason(),
            review.getOriginalRequestJson(), review.getProposedModificationJson(), review.getReviewedBy(),
            review.getReviewedAt(), review.getCustomerResponse(), review.getCustomerRespondedAt(), review.getCreatedAt());
    }
}
