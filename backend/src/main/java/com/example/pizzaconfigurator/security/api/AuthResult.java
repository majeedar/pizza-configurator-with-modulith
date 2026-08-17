package com.example.pizzaconfigurator.security.api;

public record AuthResult(CustomerView customer, String token) {
}
