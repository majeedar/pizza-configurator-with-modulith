package com.example.pizzaconfigurator.admin.web.dto;

import jakarta.validation.constraints.NotBlank;

public record AppLinkUpdateRequest(@NotBlank String url, boolean active) {
}
