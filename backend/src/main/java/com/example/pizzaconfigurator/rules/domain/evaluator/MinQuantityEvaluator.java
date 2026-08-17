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
 * The inverse of {@link MaxQuantityEvaluator}: a floor, not a ceiling, on how
 * many portions of an extra can be selected. Only fires once the customer has
 * actually chosen to add the ingredient (quantity {@literal >} 0) — an extra
 * nobody selected isn't a violation of its own minimum; that would be a
 * {@code REQUIRES} rule's job, not this one's.
 */
@Component
public class MinQuantityEvaluator implements RuleEvaluator {

    private final JsonMapper jsonMapper;

    public MinQuantityEvaluator(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public record Params(String ingredientCode, int min) {
    }

    @Override
    public boolean supports(RuleDefinition rule) {
        return rule.getRuleType() == RuleType.MIN_QUANTITY;
    }

    @Override
    public List<Violation> evaluate(RuleDefinition rule, ConfigurationCandidate configuration, RuleEvaluationContext context) {
        Params params = jsonMapper.readValue(rule.getParametersJson(), Params.class);
        int quantity = configuration.extraQuantityOf(params.ingredientCode());

        if (quantity > 0 && quantity < params.min()) {
            return List.of(new Violation(
                "MIN_QUANTITY_NOT_MET",
                "extras." + params.ingredientCode(),
                rule.getRuleCode(),
                rule.getMessage()
            ));
        }
        return List.of();
    }
}
