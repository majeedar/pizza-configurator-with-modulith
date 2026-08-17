package com.example.pizzaconfigurator.configuration.api;

import java.util.UUID;

public class ConfigurationNotFoundException extends RuntimeException {

    public ConfigurationNotFoundException(UUID configurationId) {
        super("No configuration session found for id " + configurationId);
    }
}
