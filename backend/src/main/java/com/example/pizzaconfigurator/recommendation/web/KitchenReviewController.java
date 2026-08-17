package com.example.pizzaconfigurator.recommendation.web;

import com.example.pizzaconfigurator.recommendation.api.RecommendationDecision;
import com.example.pizzaconfigurator.recommendation.api.RecommendationQuery;
import com.example.pizzaconfigurator.recommendation.api.ReviewOutcome;
import com.example.pizzaconfigurator.recommendation.api.ReviewRequestView;
import com.example.pizzaconfigurator.recommendation.web.dto.ConfigurationPatchRequest;
import com.example.pizzaconfigurator.recommendation.web.dto.RejectRequest;
import com.example.pizzaconfigurator.security.api.StaffPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Agent.md §9.2/§8.2: the kitchen review queue and Accept/Recommend/Reject
 * decision API — reachable by {@code ROLE_KITCHEN}/{@code ROLE_ADMIN} via
 * the same path-based Spring Security rule that already covers every
 * other {@code /api/v1/kitchen/**} endpoint (§14.2), regardless of which
 * module's controller registers the path.
 */
@RestController
@RequestMapping("/api/v1/kitchen/reviews")
class KitchenReviewController {

    private final RecommendationDecision recommendationDecision;
    private final RecommendationQuery recommendationQuery;

    KitchenReviewController(RecommendationDecision recommendationDecision, RecommendationQuery recommendationQuery) {
        this.recommendationDecision = recommendationDecision;
        this.recommendationQuery = recommendationQuery;
    }

    @GetMapping
    List<ReviewRequestView> queue() {
        return recommendationQuery.findQueue();
    }

    @GetMapping("/{reviewId}")
    ReviewRequestView get(@PathVariable UUID reviewId) {
        return recommendationQuery.getReviewRequest(reviewId);
    }

    @PostMapping("/{reviewId}/accept")
    ReviewOutcome accept(@PathVariable UUID reviewId, Authentication authentication) {
        return recommendationDecision.accept(reviewId, reviewedBy(authentication));
    }

    @PostMapping("/{reviewId}/recommend")
    ReviewOutcome recommend(@PathVariable UUID reviewId, @Valid @RequestBody ConfigurationPatchRequest request, Authentication authentication) {
        return recommendationDecision.recommend(reviewId, reviewedBy(authentication), request.toConfigurationPatch());
    }

    @PostMapping("/{reviewId}/reject")
    ReviewOutcome reject(@PathVariable UUID reviewId, @RequestBody(required = false) RejectRequest request, Authentication authentication) {
        String reason = request == null ? null : request.reason();
        return recommendationDecision.reject(reviewId, reviewedBy(authentication), reason);
    }

    private String reviewedBy(Authentication authentication) {
        return ((StaffPrincipal) authentication.getPrincipal()).username();
    }
}
