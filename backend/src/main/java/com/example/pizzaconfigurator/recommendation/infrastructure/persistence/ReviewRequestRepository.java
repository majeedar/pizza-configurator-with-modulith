package com.example.pizzaconfigurator.recommendation.infrastructure.persistence;

import com.example.pizzaconfigurator.recommendation.domain.ReviewRequest;
import com.example.pizzaconfigurator.recommendation.domain.ReviewRequestStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRequestRepository extends JpaRepository<ReviewRequest, UUID> {

    List<ReviewRequest> findByStatusInOrderByCreatedAtAsc(List<ReviewRequestStatus> statuses);

    Optional<ReviewRequest> findFirstByConfigurationIdOrderByCreatedAtDesc(UUID configurationId);
}
