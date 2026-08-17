package com.example.pizzaconfigurator.configuration.application;

import com.example.pizzaconfigurator.configuration.api.ConfigurationSessionView;
import com.example.pizzaconfigurator.rules.api.ConfigurationSuggestion;
import com.example.pizzaconfigurator.rules.api.Violation;
import java.util.List;

public record ValidationOutcome(
    ConfigurationSessionView session,
    List<Violation> violations,
    List<ConfigurationSuggestion> suggestions
) {
}
