package com.example.pizzaconfigurator.catalog.infrastructure.persistence;

import com.example.pizzaconfigurator.catalog.domain.Pizza;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PizzaRepository extends JpaRepository<Pizza, UUID> {

    List<Pizza> findByActiveTrue();

    Optional<Pizza> findByCode(String code);
}
