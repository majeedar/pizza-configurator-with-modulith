package com.example.pizzaconfigurator.rules.domain.evaluator;

import static com.example.pizzaconfigurator.rules.domain.evaluator.RuleTestSupport.JSON;
import static com.example.pizzaconfigurator.rules.domain.evaluator.RuleTestSupport.base;
import static com.example.pizzaconfigurator.rules.domain.evaluator.RuleTestSupport.candidate;
import static com.example.pizzaconfigurator.rules.domain.evaluator.RuleTestSupport.context;
import static com.example.pizzaconfigurator.rules.domain.evaluator.RuleTestSupport.rule;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.pizzaconfigurator.rules.api.ConfigurationCandidate;
import com.example.pizzaconfigurator.rules.api.ExtraSelection;
import com.example.pizzaconfigurator.rules.api.Violation;
import com.example.pizzaconfigurator.rules.domain.RuleDefinition;
import com.example.pizzaconfigurator.rules.domain.RuleEvaluationContext;
import com.example.pizzaconfigurator.rules.domain.RuleType;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class IngredientCompatibilityEvaluatorTest {

    private final IngredientCompatibilityEvaluator evaluator = new IngredientCompatibilityEvaluator(JSON);
    private final RuleDefinition noHamOnGlutenFree = rule(
        "NO_HAM_ON_GLUTEN_FREE", RuleType.INGREDIENT_COMPATIBILITY,
        "{\"doughCode\":\"GLUTEN_FREE\",\"incompatibleIngredientCode\":\"HAM\"}");
    private final RuleEvaluationContext context = context("HAWAII", base("HAM", true));

    @Test
    void ingredientOnCompatibleDoughIsValid() {
        ConfigurationCandidate candidate = candidate("M", "CLASSIC", Set.of(), new ExtraSelection("HAM", 1));

        assertThat(evaluator.evaluate(noHamOnGlutenFree, candidate, context)).isEmpty();
    }

    @Test
    void ingredientOnIncompatibleDoughIsInvalid() {
        ConfigurationCandidate candidate = candidate("M", "GLUTEN_FREE", Set.of(), new ExtraSelection("HAM", 1));

        List<Violation> violations = evaluator.evaluate(noHamOnGlutenFree, candidate, context);

        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).code()).isEqualTo("INGREDIENT_DOUGH_INCOMPATIBLE");
    }

    @Test
    void baseIngredientNotRemovedCountsAsPresent() {
        ConfigurationCandidate candidate = candidate("M", "GLUTEN_FREE", Set.of());

        List<Violation> violations = evaluator.evaluate(noHamOnGlutenFree, candidate, context);

        assertThat(violations).hasSize(1);
    }
}
