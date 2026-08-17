package com.example.pizzaconfigurator.security.api;

public class UsernameAlreadyRegisteredException extends RuntimeException {

    public UsernameAlreadyRegisteredException(String username) {
        super("Username already registered: " + username);
    }
}
