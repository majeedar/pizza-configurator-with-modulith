package com.example.pizzaconfigurator.rules.infrastructure.persistence;

import com.example.pizzaconfigurator.rules.domain.RuleDefinition;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RuleRepository extends JpaRepository<RuleDefinition, UUID> {

    List<RuleDefinition> findByActiveTrue();
}
