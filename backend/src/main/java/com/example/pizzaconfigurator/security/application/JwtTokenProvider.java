package com.example.pizzaconfigurator.security.application;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Self-issued JWT (agent.md §14.1) — no external identity provider. Staff
 * tokens (Phase 7) reuse this same provider, distinguished from customer
 * tokens by the presence of a {@code role} claim.
 */
@Component
class JwtTokenProvider {

    private static final String ROLE_CLAIM = "role";
    private static final String USERNAME_CLAIM = "username";

    private final SecretKey key;
    private final String issuer;
    private final String audience;
    private final Duration customerTtl;
    private final Duration staffTtl;
    private final Clock clock;

    JwtTokenProvider(
        @Value("${app.jwt.secret}") String secret,
        @Value("${app.jwt.issuer}") String issuer,
        @Value("${app.jwt.audience}") String audience,
        @Value("${app.jwt.customer-token-ttl-minutes}") long customerTtlMinutes,
        @Value("${app.jwt.staff-token-ttl-minutes}") long staffTtlMinutes,
        Clock clock
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
        this.audience = audience;
        this.customerTtl = Duration.ofMinutes(customerTtlMinutes);
        this.staffTtl = Duration.ofMinutes(staffTtlMinutes);
        this.clock = clock;
    }

    String issueToken(UUID subject) {
        Instant now = Instant.now(clock);
        return Jwts.builder()
            .subject(subject.toString())
            .issuer(issuer)
            .audience().add(audience).and()
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(customerTtl)))
            .signWith(key)
            .compact();
    }

    Optional<UUID> parseSubject(String token) {
        return parseClaims(token).map(claims -> UUID.fromString(claims.getSubject()));
    }

    String issueStaffToken(UUID employeeId, String username, String role) {
        Instant now = Instant.now(clock);
        return Jwts.builder()
            .subject(employeeId.toString())
            .issuer(issuer)
            .audience().add(audience).and()
            .claim(USERNAME_CLAIM, username)
            .claim(ROLE_CLAIM, role)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(staffTtl)))
            .signWith(key)
            .compact();
    }

    Optional<StaffClaims> parseStaffClaims(String token) {
        return parseClaims(token).flatMap(claims -> {
            String role = claims.get(ROLE_CLAIM, String.class);
            String username = claims.get(USERNAME_CLAIM, String.class);
            if (role == null || username == null) {
                return Optional.empty();
            }
            return Optional.of(new StaffClaims(UUID.fromString(claims.getSubject()), username, role));
        });
    }

    private Optional<Claims> parseClaims(String token) {
        try {
            return Optional.of(Jwts.parser()
                .verifyWith(key)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    record StaffClaims(UUID employeeId, String username, String role) {
    }
}
