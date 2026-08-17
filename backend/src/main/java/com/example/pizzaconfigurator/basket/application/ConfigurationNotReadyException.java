package com.example.pizzaconfigurator.basket.application;

import java.util.UUID;

/**
 * "Basket must never bypass validation/pricing" (agent.md §7.6) — thrown
 * when a caller tries to add a configuration that isn't valid and priced
 * yet.
 */
public class ConfigurationNotReadyException extends RuntimeException {

    public ConfigurationNotReadyException(UUID configurationId) {
        super("Configuration " + configurationId + " is not valid and priced yet — cannot add to basket");
    }
}
