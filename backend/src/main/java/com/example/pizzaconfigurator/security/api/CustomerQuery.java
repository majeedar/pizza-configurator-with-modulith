package com.example.pizzaconfigurator.security.api;

import java.util.Optional;
import java.util.UUID;

/**
 * Read-side of the customer slice, kept separate from
 * {@link CustomerAuthentication} (a login/registration concern). Used by
 * the {@code notification} module (agent.md §7.9) to resolve a registered
 * customer's email address — guest orders have no {@code customerId} and
 * so are never looked up here (agent.md §21 Scenario J: "the customer also
 * has an email address").
 */
public interface CustomerQuery {

    Optional<CustomerView> findCustomer(UUID customerId);
}
