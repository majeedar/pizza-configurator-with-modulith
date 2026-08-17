package com.example.pizzaconfigurator.catalog.infrastructure.persistence;

import com.example.pizzaconfigurator.catalog.domain.PizzaIngredient;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PizzaIngredientRepository extends JpaRepository<PizzaIngredient, UUID> {

    /**
     * {@code JOIN FETCH} the lazy {@code ingredient} association in the same
     * query: with {@code spring.jpa.open-in-view: false}, the Hibernate
     * session closes when this (transactional) repository call returns, so a
     * caller mapping to a DTO afterward — as {@code CatalogAdminController}
     * does — would otherwise hit a {@code LazyInitializationException} the
     * first time it touched {@code line.getIngredient()}.
     */
    @Query("select pi from PizzaIngredient pi join fetch pi.ingredient where pi.pizza.pizzaId = :pizzaId")
    List<PizzaIngredient> findByPizza_PizzaId(UUID pizzaId);

    /** Same lazy-association concern as above, for the single-line lookup {@code updateRecipeLine} uses. */
    @Query("select pi from PizzaIngredient pi join fetch pi.ingredient where pi.pizzaIngredientId = :pizzaIngredientId")
    Optional<PizzaIngredient> findWithIngredientById(UUID pizzaIngredientId);
}
