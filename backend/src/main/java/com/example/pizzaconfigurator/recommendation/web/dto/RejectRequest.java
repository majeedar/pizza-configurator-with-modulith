package com.example.pizzaconfigurator.recommendation.web.dto;

import jakarta.validation.constraints.Size;

public record RejectRequest(@Size(max = 1000) String reason) {
}
