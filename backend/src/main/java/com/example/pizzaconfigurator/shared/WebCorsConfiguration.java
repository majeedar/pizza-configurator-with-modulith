package com.example.pizzaconfigurator.shared;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * CORS allowlist (agent.md §14.4) — the customer/staff web apps run on
 * different origins/ports than the backend. Exposed as a
 * {@link CorsConfigurationSource} rather than a {@code WebMvcConfigurer} so
 * the Spring Security filter chain (see {@code security} module, Phase 7)
 * applies it to every request, including ones the security filter chain
 * itself rejects before MVC dispatch — a single source avoids the
 * duplicate-header conflicts that come from configuring CORS in two places.
 */
@Configuration
class WebCorsConfiguration {

    private final List<String> allowedOrigins;

    WebCorsConfiguration(@Value("${app.cors.allowed-origins}") String allowedOrigins) {
        this.allowedOrigins = List.of(allowedOrigins.split(","));
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
