package com.example.pizzaconfigurator.orders.infrastructure.persistence;

import com.example.pizzaconfigurator.orders.domain.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, String> {
}
