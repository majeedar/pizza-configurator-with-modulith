package com.example.pizzaconfigurator.security.web;

import com.example.pizzaconfigurator.security.api.StaffAuthentication;
import com.example.pizzaconfigurator.security.api.StaffPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Populates the {@link SecurityContextHolder} from a staff bearer JWT
 * (agent.md §14.1) so the authorization rules in
 * {@link SecurityFilterChainConfiguration} can enforce {@code ROLE_KITCHEN}
 * / {@code ROLE_ADMIN}. Customer/guest requests never carry a role claim,
 * so this filter simply leaves them unauthenticated and lets the
 * {@code permitAll} rule for public endpoints handle them — it does not
 * replace the manual {@code CustomerAuthentication.resolveCustomerId}
 * resolution used elsewhere.
 */
class StaffJwtAuthenticationFilter extends OncePerRequestFilter {

    private final StaffAuthentication staffAuthentication;

    StaffJwtAuthenticationFilter(StaffAuthentication staffAuthentication) {
        this.staffAuthentication = staffAuthentication;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        bearerToken(request)
            .flatMap(staffAuthentication::resolveStaff)
            .ifPresent(this::authenticate);
        filterChain.doFilter(request, response);
    }

    private void authenticate(StaffPrincipal principal) {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + principal.role().name()));
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private Optional<String> bearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return Optional.empty();
        }
        return Optional.of(header.substring("Bearer ".length()));
    }
}
