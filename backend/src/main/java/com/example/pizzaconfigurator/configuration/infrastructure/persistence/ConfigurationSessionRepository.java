package com.example.pizzaconfigurator.configuration.infrastructure.persistence;

import com.example.pizzaconfigurator.configuration.domain.ConfigurationSession;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfigurationSessionRepository extends JpaRepository<ConfigurationSession, UUID> {
}
