package com.example.pizzaconfigurator.orders.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

/**
 * Opaque guest order-access token (agent.md §14.3) — the raw token is
 * returned to the caller exactly once (at checkout) and never stored;
 * only its SHA-256 hash is persisted on {@code Order.accessTokenHash}, and
 * lookups re-hash the caller-supplied token to compare against it.
 */
@Component
class AccessTokenGenerator {

    private final SecureRandom random = new SecureRandom();

    record Generated(String rawToken, String hash) {
    }

    Generated generate() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return new Generated(rawToken, hash(rawToken));
    }

    String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
