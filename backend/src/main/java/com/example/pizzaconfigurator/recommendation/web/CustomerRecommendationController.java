package com.example.pizzaconfigurator.recommendation.web;

import com.example.pizzaconfigurator.recommendation.api.NoPendingRecommendationException;
import com.example.pizzaconfigurator.recommendation.api.RecommendationResponse;
import com.example.pizzaconfigurator.recommendation.api.RecommendationQuery;
import com.example.pizzaconfigurator.recommendation.api.ReviewOutcome;
import com.example.pizzaconfigurator.recommendation.api.ReviewRequestView;
import com.example.pizzaconfigurator.recommendation.domain.ReviewRequestStatus;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Agent.md §9.1: customer-facing review/recommendation polling and
 * response, under the same {@code /configurations/{configurationId}}
 * namespace as {@code configuration.web} even though this controller
 * belongs to the {@code recommendation} module — Spring MVC route
 * registration doesn't require the URL path to match the owning module.
 */
@RestController
@RequestMapping("/api/v1/configurations/{configurationId}")
class CustomerRecommendationController {

    private final RecommendationResponse recommendationResponse;
    private final RecommendationQuery recommendationQuery;

    CustomerRecommendationController(RecommendationResponse recommendationResponse, RecommendationQuery recommendationQuery) {
        this.recommendationResponse = recommendationResponse;
        this.recommendationQuery = recommendationQuery;
    }

    @GetMapping("/review-status")
    ReviewRequestView reviewStatus(@PathVariable UUID configurationId) {
        return recommendationQuery.findLatestForConfiguration(configurationId)
            .orElseThrow(() -> new NoPendingRecommendationException(configurationId));
    }

    @GetMapping("/recommendation")
    ReviewRequestView recommendation(@PathVariable UUID configurationId) {
        return pendingRecommendation(configurationId);
    }

    @PostMapping("/recommendation/accept")
    ReviewOutcome accept(@PathVariable UUID configurationId) {
        return recommendationResponse.acceptRecommendation(pendingRecommendation(configurationId).reviewRequestId());
    }

    @PostMapping("/recommendation/reject")
    ReviewOutcome reject(@PathVariable UUID configurationId) {
        return recommendationResponse.rejectRecommendation(pendingRecommendation(configurationId).reviewRequestId());
    }

    private ReviewRequestView pendingRecommendation(UUID configurationId) {
        return recommendationQuery.findLatestForConfiguration(configurationId)
            .filter(view -> view.status() == ReviewRequestStatus.RECOMMENDED_BY_KITCHEN)
            .orElseThrow(() -> new NoPendingRecommendationException(configurationId));
    }
}
