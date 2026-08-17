package com.example.pizzaconfigurator.orders.application;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/**
 * Short code the customer reads aloud / shows at the counter to collect
 * their order (agent.md §5.1, §14.3) — deliberately distinct from the
 * unguessable online {@link AccessTokenGenerator} access token.
 */
@Component
class PickupTokenGenerator {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int LENGTH = 6;

    private final SecureRandom random = new SecureRandom();

    String next() {
        StringBuilder token = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            token.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return token.toString();
    }
}
