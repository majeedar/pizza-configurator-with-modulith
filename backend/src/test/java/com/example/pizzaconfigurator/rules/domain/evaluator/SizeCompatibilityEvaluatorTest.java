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

class SizeCompatibilityEvaluatorTest {

    private final SizeCompatibilityEvaluator evaluator = new SizeCompatibilityEvaluator(JSON);
    private final RuleDefinition noLargeGlutenFree =
        rule("NO_L_SIZE", RuleType.SIZE_COMPATIBILITY, "{\"sizeCode\":\"L\",\"allowed\":false}");
    private final RuleEvaluationContext context = context("MARGHERITA");

    @Test
    void allowedSizeIsValid() {
        ConfigurationCandidate candidate = candidate("M", "CLASSIC", Set.of());

        assertThat(evaluator.evaluate(noLargeGlutenFree, candidate, context)).isEmpty();
    }

    @Test
    void disallowedSizeIsInvalid() {
        ConfigurationCandidate candidate = candidate("L", "CLASSIC", Set.of());

        List<Violation> violations = evaluator.evaluate(noLargeGlutenFree, candidate, context);

        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).code()).isEqualTo("SIZE_NOT_AVAILABLE");
    }
}
