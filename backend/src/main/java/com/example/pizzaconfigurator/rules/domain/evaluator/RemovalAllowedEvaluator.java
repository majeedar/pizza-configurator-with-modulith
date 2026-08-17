package com.example.pizzaconfigurator.rules.domain.evaluator;

import com.example.pizzaconfigurator.rules.api.ConfigurationCandidate;
import com.example.pizzaconfigurator.rules.api.Violation;
import com.example.pizzaconfigurator.rules.domain.RuleDefinition;
import com.example.pizzaconfigurator.rules.domain.RuleEvaluationContext;
import com.example.pizzaconfigurator.rules.domain.RuleEvaluator;
import com.example.pizzaconfigurator.rules.domain.RuleType;
import java.util.List;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class RemovalAllowedEvaluator implements RuleEvaluator {

    private final JsonMapper jsonMapper;

    public RemovalAllowedEvaluator(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public record Params(String ingredientCode, boolean removable) {
    }

    @Override
    public boolean supports(RuleDefinition rule) {
        return rule.getRuleType() == RuleType.REMOVAL_ALLOWED;
    }

    @Override
    public List<Violation> evaluate(RuleDefinition rule, ConfigurationCandidate configuration, RuleEvaluationContext context) {
        Params params = jsonMapper.readValue(rule.getParametersJson(), Params.class);
        boolean wasRemoved = configuration.removedIngredientCodes().contains(params.ingredientCode());

        if (wasRemoved && !params.removable()) {
            return List.of(new Violation(
                "REMOVAL_NOT_ALLOWED",
                "removedIngredients." + params.ingredientCode(),
                rule.getRuleCode(),
                rule.getMessage()
            ));
        }
        return List.of();
    }
}
