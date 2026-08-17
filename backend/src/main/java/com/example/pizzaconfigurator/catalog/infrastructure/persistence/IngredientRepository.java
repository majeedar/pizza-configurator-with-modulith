package com.example.pizzaconfigurator.catalog.infrastructure.persistence;

import com.example.pizzaconfigurator.catalog.domain.Ingredient;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngredientRepository extends JpaRepository<Ingredient, UUID> {

    List<Ingredient> findByActiveTrue();

    Optional<Ingredient> findByCode(String code);
}
