package com.example.pizzaconfigurator.configuration.application;

public class ConfigurationNotValidException extends RuntimeException {

    public ConfigurationNotValidException() {
        super("Configuration must be successfully validated before it can be priced");
    }
}
