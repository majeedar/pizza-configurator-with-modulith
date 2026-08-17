package com.example.pizzaconfigurator.pricing.infrastructure.persistence;

import com.example.pizzaconfigurator.pricing.domain.ItemType;
import com.example.pizzaconfigurator.pricing.domain.PriceDefinition;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceDefinitionRepository extends JpaRepository<PriceDefinition, UUID> {

    List<PriceDefinition> findByItemTypeAndItemIdAndActiveTrue(ItemType itemType, String itemId);

    default Optional<PriceDefinition> findActive(ItemType itemType, String itemId) {
        return findByItemTypeAndItemIdAndActiveTrue(itemType, itemId).stream().findFirst();
    }
}
