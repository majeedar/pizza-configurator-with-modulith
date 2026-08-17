package com.example.pizzaconfigurator.security.web.dto;

import jakarta.validation.constraints.NotBlank;

public record StaffLoginRequest(
    @NotBlank String username,
    @NotBlank String password
) {
}
