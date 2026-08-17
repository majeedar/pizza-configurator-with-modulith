package com.example.pizzaconfigurator.configuration.application;

public class ConfigurationExpiredException extends RuntimeException {

    public ConfigurationExpiredException() {
        super("This configuration session has expired — please start again");
    }
}
