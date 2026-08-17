package com.example.pizzaconfigurator.rules.api;

public record Violation(
    String code,
    String field,
    String ruleCode,
    String message
) {
}
