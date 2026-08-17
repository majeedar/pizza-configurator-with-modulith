package com.example.pizzaconfigurator.security.api;

import java.util.Optional;
import java.util.UUID;

/**
 * Published API of the security module's customer-account slice (agent.md
 * §14.1). Guest checkout does not go through this at all — it's only used
 * when a customer registers/logs in, and by other modules that need to
 * resolve "who is the caller" from a bearer token.
 */
public interface CustomerAuthentication {

    AuthResult register(String name, String email, String phoneNumber, String rawPassword);

    AuthResult login(String email, String rawPassword);

    /**
     * Empty if the token is missing, expired, or invalid — callers treat
     * that the same as an unauthenticated/guest request, not an error.
     */
    Optional<UUID> resolveCustomerId(String token);
}
