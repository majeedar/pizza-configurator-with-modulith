package com.example.pizzaconfigurator.security.api;

import java.util.Optional;

/**
 * Published API of the security module's staff-account slice (agent.md
 * §14.1). {@link #resolveStaff} is consumed by the Spring Security filter
 * chain that gates {@code /api/v1/kitchen/**} and {@code /api/v1/admin/**}.
 */
public interface StaffAuthentication {

    StaffAuthResult login(String username, String rawPassword);

    /**
     * Empty if the token is missing, expired, invalid, or the account has
     * since been disabled — treated as unauthenticated, not an error.
     */
    Optional<StaffPrincipal> resolveStaff(String token);
}
