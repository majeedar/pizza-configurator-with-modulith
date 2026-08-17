package com.example.pizzaconfigurator.shared;

import com.example.pizzaconfigurator.security.api.StaffPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Resolves "who's making this admin change" for audit logging (agent.md
 * §14.4/§30) without threading an actor parameter through every existing
 * admin service method signature — safe to call from any {@code
 * /api/v1/admin/**} or {@code /api/v1/kitchen/**} request thread, since
 * those are the only paths the Spring Security filter chain authenticates
 * (agent.md §14.2).
 */
public final class CurrentStaffActor {

    private CurrentStaffActor() {
    }

    public static String username() {
        return principal().username();
    }

    public static String role() {
        return principal().roleName();
    }

    private static StaffPrincipal principal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof StaffPrincipal staffPrincipal)) {
            throw new IllegalStateException("No authenticated staff principal on this request");
        }
        return staffPrincipal;
    }
}
