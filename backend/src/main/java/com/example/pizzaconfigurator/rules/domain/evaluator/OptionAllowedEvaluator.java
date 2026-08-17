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
 * A flat gate on whether a single ingredient/extra option can be selected at
 * all, independent of size or dough — the one axis {@link SizeCompatibilityEvaluator}
 * (size), {@link DoughCompatibilityEvaluator} (dough), {@link IngredientCompatibilityEvaluator}/
 * {@link ExclusionEvaluator} (ingredient-vs-ingredient/dough pairs), and
 * {@link RemovalAllowedEvaluator} (removability of a base ingredient) don't
 * already cover. Mirrors the size/dough compatibility evaluators' {@code
 * (code, allowed)} shape and only-fires-when-disallowed semantics. Uses
 * {@link RuleEvaluationContext#isIngredientPresent} rather than just {@code
 * hasExtra} so an outright ban also catches the ingredient showing up as a
 * non-removed base ingredient, not only as an added extra.
 */
@Component
public class OptionAllowedEvaluator implements RuleEvaluator {

    private final JsonMapper jsonMapper;

    public OptionAllowedEvaluator(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public record Params(String ingredientCode, boolean allowed) {
    }

    @Override
    public boolean supports(RuleDefinition rule) {
        return rule.getRuleType() == RuleType.OPTION_ALLOWED;
    }

    @Override
    public List<Violation> evaluate(RuleDefinition rule, ConfigurationCandidate configuration, RuleEvaluationContext context) {
        Params params = jsonMapper.readValue(rule.getParametersJson(), Params.class);

        if (!params.allowed() && context.isIngredientPresent(configuration, params.ingredientCode())) {
            return List.of(new Violation(
                "OPTION_NOT_ALLOWED",
                "extras." + params.ingredientCode(),
                rule.getRuleCode(),
                rule.getMessage()
            ));
        }
        return List.of();
    }
}
