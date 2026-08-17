package com.example.pizzaconfigurator.rules.domain.evaluator;

import com.example.pizzaconfigurator.catalog.api.ConfigurableOptions;
import com.example.pizzaconfigurator.catalog.api.RecipeItemView;
import com.example.pizzaconfigurator.rules.api.ConfigurationCandidate;
import com.example.pizzaconfigurator.rules.api.ExtraSelection;
import com.example.pizzaconfigurator.rules.domain.RuleDefinition;
import com.example.pizzaconfigurator.rules.domain.RuleEvaluationContext;
import com.example.pizzaconfigurator.rules.domain.RuleType;
import com.example.pizzaconfigurator.rules.domain.ScopeType;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import tools.jackson.databind.json.JsonMapper;

/**
 * Shared fixtures for evaluator unit tests — no Spring context, no database
 * (agent.md §11: "Rules must be testable without HTTP or database access").
 */
final class RuleTestSupport {

    static final JsonMapper JSON = JsonMapper.builder().build();

    private RuleTestSupport() {
    }

    static RuleDefinition rule(String code, RuleType type, String parametersJson) {
        return new RuleDefinition(code, type, ScopeType.GLOBAL, null, parametersJson, "violated: " + code, true, null, null);
    }

    static RuleEvaluationContext context(String pizzaCode, RecipeItemView... baseIngredients) {
        ConfigurableOptions options = new ConfigurableOptions(
            UUID.randomUUID(), pizzaCode, List.of(baseIngredients), List.of(), List.of(), List.of());
        return new RuleEvaluationContext(pizzaCode, options);
    }

    static RecipeItemView base(String code, boolean removable) {
        return new RecipeItemView(code, code, 1, removable);
    }

    static ConfigurationCandidate candidate(
        String sizeCode, String doughCode, Set<String> removed, ExtraSelection... extras
    ) {
        return new ConfigurationCandidate(UUID.randomUUID(), sizeCode, doughCode, removed, List.of(extras));
    }
}
