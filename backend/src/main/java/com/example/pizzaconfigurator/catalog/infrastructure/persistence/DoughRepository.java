package com.example.pizzaconfigurator.catalog.infrastructure.persistence;

import com.example.pizzaconfigurator.catalog.domain.Dough;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoughRepository extends JpaRepository<Dough, UUID> {

    List<Dough> findByActiveTrue();

    Optional<Dough> findByCode(String code);
}
