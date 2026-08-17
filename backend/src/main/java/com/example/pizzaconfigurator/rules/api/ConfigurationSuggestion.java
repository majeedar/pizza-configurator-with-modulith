package com.example.pizzaconfigurator.rules.api;

import java.util.Map;

public record ConfigurationSuggestion(
    String description,
    Map<String, Object> patch
) {
}
