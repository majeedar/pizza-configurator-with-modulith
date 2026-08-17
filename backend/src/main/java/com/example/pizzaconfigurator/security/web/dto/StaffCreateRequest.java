package com.example.pizzaconfigurator.security.web.dto;

import com.example.pizzaconfigurator.security.domain.EmployeeRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StaffCreateRequest(
    @NotBlank String username,
    @NotBlank String displayName,
    String email,
    @NotBlank String password,
    @NotNull EmployeeRole role
) {
}
