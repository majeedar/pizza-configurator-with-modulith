package com.example.pizzaconfigurator.security.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerRegisterRequest(
    @NotBlank String name,
    @NotBlank @Email String email,
    String phoneNumber,
    @NotBlank @Size(min = 8) String password
) {
}
