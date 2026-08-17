package com.example.pizzaconfigurator.security.api;

import java.util.UUID;

public record CustomerView(UUID customerId, String name, String email) {
}
