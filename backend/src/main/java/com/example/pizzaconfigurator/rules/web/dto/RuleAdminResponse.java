package com.example.pizzaconfigurator.rules.web.dto;

import com.example.pizzaconfigurator.rules.domain.RuleType;
import com.example.pizzaconfigurator.rules.domain.ScopeType;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record RuleAdminResponse(
    UUID ruleId,
    String ruleCode,
    RuleType ruleType,
    ScopeType scopeType,
    String scopeId,
    Map<String, Object> parameters,
    String message,
    boolean active,
    Long version,
    Instant validFrom,
    Instant validTo
) {
}
