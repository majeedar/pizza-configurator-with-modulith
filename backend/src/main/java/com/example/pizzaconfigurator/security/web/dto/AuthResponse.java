package com.example.pizzaconfigurator.security.web.dto;

import java.util.UUID;

public record AuthResponse(UUID customerId, String name, String email, String token) {
}
