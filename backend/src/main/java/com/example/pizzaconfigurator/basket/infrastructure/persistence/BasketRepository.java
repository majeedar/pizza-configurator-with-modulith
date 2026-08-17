package com.example.pizzaconfigurator.basket.infrastructure.persistence;

import com.example.pizzaconfigurator.basket.domain.Basket;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BasketRepository extends JpaRepository<Basket, UUID> {
}
