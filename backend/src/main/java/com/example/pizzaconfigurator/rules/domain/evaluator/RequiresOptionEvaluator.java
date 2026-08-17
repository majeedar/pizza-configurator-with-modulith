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
 * "If ingredient A is present, ingredient B must be present too."
 */
@Component
public class RequiresOptionEvaluator implements RuleEvaluator {

    private final JsonMapper jsonMapper;

    public RequiresOptionEvaluator(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public record Params(String ifIngredientCode, String thenRequiresIngredientCode) {
    }

    @Override
    public boolean supports(RuleDefinition rule) {
        return rule.getRuleType() == RuleType.REQUIRES;
    }

    @Override
    public List<Violation> evaluate(RuleDefinition rule, ConfigurationCandidate configuration, RuleEvaluationContext context) {
        Params params = jsonMapper.readValue(rule.getParametersJson(), Params.class);

        boolean triggerPresent = context.isIngredientPresent(configuration, params.ifIngredientCode());
        boolean requiredPresent = context.isIngredientPresent(configuration, params.thenRequiresIngredientCode());

        if (triggerPresent && !requiredPresent) {
            return List.of(new Violation(
                "REQUIRED_INGREDIENT_MISSING",
                "extras." + params.thenRequiresIngredientCode(),
                rule.getRuleCode(),
                rule.getMessage()
            ));
        }
        return List.of();
    }
}
