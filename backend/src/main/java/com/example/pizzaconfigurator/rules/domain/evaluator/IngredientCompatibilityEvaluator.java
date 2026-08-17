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

/**
 * "This ingredient cannot be combined with that dough" (e.g. a topping that
 * doesn't work on a gluten-free base).
 */
@Component
public class IngredientCompatibilityEvaluator implements RuleEvaluator {

    private final JsonMapper jsonMapper;

    public IngredientCompatibilityEvaluator(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public record Params(String doughCode, String incompatibleIngredientCode) {
    }

    @Override
    public boolean supports(RuleDefinition rule) {
        return rule.getRuleType() == RuleType.INGREDIENT_COMPATIBILITY;
    }

    @Override
    public List<Violation> evaluate(RuleDefinition rule, ConfigurationCandidate configuration, RuleEvaluationContext context) {
        Params params = jsonMapper.readValue(rule.getParametersJson(), Params.class);

        boolean doughMatches = configuration.doughCode().equals(params.doughCode());
        boolean ingredientPresent = context.isIngredientPresent(configuration, params.incompatibleIngredientCode());

        if (doughMatches && ingredientPresent) {
            return List.of(new Violation(
                "INGREDIENT_DOUGH_INCOMPATIBLE",
                "extras." + params.incompatibleIngredientCode(),
                rule.getRuleCode(),
                rule.getMessage()
            ));
        }
        return List.of();
    }
}
