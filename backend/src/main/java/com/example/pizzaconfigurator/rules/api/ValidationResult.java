package com.example.pizzaconfigurator.rules.api;

import java.util.List;

public record ValidationResult(
    ValidationStatus status,
    String ruleVersion,
    List<Violation> violations,
    List<ConfigurationSuggestion> suggestions
) {
}
