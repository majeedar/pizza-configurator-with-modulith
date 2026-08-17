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
 * "Ingredient A and ingredient B cannot both be present."
 */
@Component
public class ExclusionEvaluator implements RuleEvaluator {

    private final JsonMapper jsonMapper;

    public ExclusionEvaluator(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public record Params(String ingredientA, String ingredientB) {
    }

    @Override
    public boolean supports(RuleDefinition rule) {
        return rule.getRuleType() == RuleType.EXCLUDES;
    }

    @Override
    public List<Violation> evaluate(RuleDefinition rule, ConfigurationCandidate configuration, RuleEvaluationContext context) {
        Params params = jsonMapper.readValue(rule.getParametersJson(), Params.class);

        boolean aPresent = context.isIngredientPresent(configuration, params.ingredientA());
        boolean bPresent = context.isIngredientPresent(configuration, params.ingredientB());

        if (aPresent && bPresent) {
            return List.of(new Violation(
                "INCOMPATIBLE_INGREDIENTS",
                "extras." + params.ingredientB(),
                rule.getRuleCode(),
                rule.getMessage()
            ));
        }
        return List.of();
    }
}
