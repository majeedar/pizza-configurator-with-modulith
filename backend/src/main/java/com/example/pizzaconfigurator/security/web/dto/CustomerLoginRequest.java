package com.example.pizzaconfigurator.security.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CustomerLoginRequest(
    @NotBlank String email,
    @NotBlank String password
) {
}
