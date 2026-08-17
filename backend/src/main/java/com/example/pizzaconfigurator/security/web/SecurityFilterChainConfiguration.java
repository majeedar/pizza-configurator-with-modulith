package com.example.pizzaconfigurator.security.web;

import com.example.pizzaconfigurator.security.api.StaffAuthentication;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import tools.jackson.databind.json.JsonMapper;

/**
 * Agent.md §14.2/§14.4: request-level RBAC, deny-by-default, stateless
 * bearer-JWT (no CSRF needed). Only {@code /api/v1/kitchen/**} and
 * {@code /api/v1/admin/**} are actually gated — every other endpoint
 * (customer/guest, catalog, staff login itself) stays {@code permitAll} at
 * this layer and keeps using the manual optional-JWT resolution
 * (agent.md §14.1) that predates this filter chain, since
 * {@code PUBLIC_CUSTOMER} is not a real authentication concept (§14.2).
 */
@Configuration
@EnableWebSecurity
class SecurityFilterChainConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, StaffAuthentication staffAuthentication, JsonMapper jsonMapper)
        throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> {
            })
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Explicit for auditability (agent.md §14.4 "secure headers") even though most of
            // these are Spring Security defaults: nosniff/frame-options/HSTS are already on by
            // default and would apply either way — this just documents that fact and tunes HSTS
            // (a year, including subdomains) plus adds a Referrer-Policy, which is not a
            // default. HSTS only takes effect on requests Spring sees as HTTPS, which behind the
            // Hetzner nginx reverse proxy means trusting X-Forwarded-Proto (server.forward-
            // headers-strategy: native in application.yml) — harmless locally where no such
            // header is ever present.
            .headers(headers -> headers
                .contentTypeOptions(contentTypeOptions -> {
                })
                .frameOptions(frameOptions -> frameOptions.deny())
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000))
                .referrerPolicy(referrerPolicy -> referrerPolicy
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/kitchen/**").hasAnyRole("KITCHEN", "ADMIN")
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .anyRequest().permitAll())
            .addFilterBefore(new StaffJwtAuthenticationFilter(staffAuthentication), UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(handling -> handling
                .authenticationEntryPoint((request, response, exception) -> writeProblem(response, jsonMapper, 401, "Unauthorized"))
                .accessDeniedHandler((request, response, exception) -> writeProblem(response, jsonMapper, 403, "Forbidden")));
        return http.build();
    }

    /**
     * Boot's {@code UserDetailsServiceAutoConfiguration} generates a random
     * default-user password and logs it on every startup unless some
     * {@link UserDetailsService} bean already exists. Nothing here uses
     * form login or HTTP Basic — staff auth is entirely JWT-based — so this
     * is a deliberately empty/unused manager, only to suppress that noise.
     */
    @Bean
    UserDetailsService noopUserDetailsService() {
        return new InMemoryUserDetailsManager();
    }

    private static void writeProblem(HttpServletResponse response, JsonMapper jsonMapper, int status, String title)
        throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write(jsonMapper.writeValueAsString(Map.of("status", status, "title", title)));
    }
}
