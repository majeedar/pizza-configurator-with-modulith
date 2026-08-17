package com.example.pizzaconfigurator.basket.infrastructure.persistence;

import com.example.pizzaconfigurator.basket.domain.BasketItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BasketItemRepository extends JpaRepository<BasketItem, UUID> {

    List<BasketItem> findByBasketId(UUID basketId);
}
