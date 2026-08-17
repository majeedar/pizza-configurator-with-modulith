package com.example.pizzaconfigurator;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Proves the application context starts and Flyway migrations apply
 * successfully against a real PostgreSQL instance (agent.md §20.3 — no H2).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(SharedTestcontainersConfiguration.class)
@Testcontainers
class PizzaConfiguratorApplicationTests {

    @Test
    void contextLoads() {
    }
}
