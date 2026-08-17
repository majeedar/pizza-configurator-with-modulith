package com.example.pizzaconfigurator.security.api;

import com.example.pizzaconfigurator.security.domain.EmployeeRole;
import java.util.UUID;

/**
 * What the Spring Security filter chain needs from a staff bearer token —
 * embedded directly in the JWT claims (agent.md §14.1) so authorization
 * doesn't require a database round-trip per request.
 */
public record StaffPrincipal(UUID employeeId, String username, EmployeeRole role) {

    /**
     * For callers outside this module (e.g. {@code shared.CurrentStaffActor})
     * that only need the role's name, not the {@code security.domain} type
     * itself — touching {@link EmployeeRole} directly from another module
     * is a Spring Modulith boundary violation even for an inherited method
     * like {@code name()}.
     */
    public String roleName() {
        return role.name();
    }
}
