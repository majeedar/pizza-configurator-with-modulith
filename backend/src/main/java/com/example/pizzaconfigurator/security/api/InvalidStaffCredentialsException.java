package com.example.pizzaconfigurator.security.api;

public class InvalidStaffCredentialsException extends RuntimeException {

    public InvalidStaffCredentialsException() {
        super("Invalid username or password");
    }
}
