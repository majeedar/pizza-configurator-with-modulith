package com.example.pizzaconfigurator.rules.web.dto;

import com.example.pizzaconfigurator.rules.domain.RuleType;
import com.example.pizzaconfigurator.rules.domain.ScopeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Map;

public record RuleAdminRequest(
    @NotBlank String ruleCode,
    @NotNull RuleType ruleType,
    @NotNull ScopeType scopeType,
    String scopeId,
    @NotNull Map<String, Object> parameters,
    @NotBlank String message,
    boolean active,
    Instant validFrom,
    Instant validTo
) {
}
