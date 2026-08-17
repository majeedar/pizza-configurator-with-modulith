package com.example.pizzaconfigurator.rules.domain.evaluator;

import static com.example.pizzaconfigurator.rules.domain.evaluator.RuleTestSupport.JSON;
import static com.example.pizzaconfigurator.rules.domain.evaluator.RuleTestSupport.candidate;
import static com.example.pizzaconfigurator.rules.domain.evaluator.RuleTestSupport.context;
import static com.example.pizzaconfigurator.rules.domain.evaluator.RuleTestSupport.rule;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.pizzaconfigurator.rules.api.ConfigurationCandidate;
import com.example.pizzaconfigurator.rules.api.Violation;
import com.example.pizzaconfigurator.rules.domain.RuleDefinition;
import com.example.pizzaconfigurator.rules.domain.RuleEvaluationContext;
import com.example.pizzaconfigurator.rules.domain.RuleType;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RemovalAllowedEvaluatorTest {

    private final RemovalAllowedEvaluator evaluator = new RemovalAllowedEvaluator(JSON);
    private final RuleDefinition sauceNotRemovable =
        rule("TOMATO_SAUCE_NOT_REMOVABLE", RuleType.REMOVAL_ALLOWED, "{\"ingredientCode\":\"TOMATO_SAUCE\",\"removable\":false}");
    private final RuleEvaluationContext context = context("MARGHERITA");

    @Test
    void leavingItOnIsValid() {
        ConfigurationCandidate candidate = candidate("M", "CLASSIC", Set.of());

        List<Violation> violations = evaluator.evaluate(sauceNotRemovable, candidate, context);

        assertThat(violations).isEmpty();
    }

    @Test
    void removingANonRemovableIngredientIsInvalid() {
        ConfigurationCandidate candidate = candidate("M", "CLASSIC", Set.of("TOMATO_SAUCE"));

        List<Violation> violations = evaluator.evaluate(sauceNotRemovable, candidate, context);

        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).code()).isEqualTo("REMOVAL_NOT_ALLOWED");
    }
}
