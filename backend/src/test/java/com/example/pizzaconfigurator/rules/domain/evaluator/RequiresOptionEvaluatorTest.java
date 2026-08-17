package com.example.pizzaconfigurator.rules.domain.evaluator;

import static com.example.pizzaconfigurator.rules.domain.evaluator.RuleTestSupport.JSON;
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

class RequiresOptionEvaluatorTest {

    private final RequiresOptionEvaluator evaluator = new RequiresOptionEvaluator(JSON);
    private final RuleDefinition hamRequiresPineapple = rule(
        "HAM_REQUIRES_PINEAPPLE", RuleType.REQUIRES,
        "{\"ifIngredientCode\":\"HAM\",\"thenRequiresIngredientCode\":\"PINEAPPLE\"}");
    private final RuleEvaluationContext context = context("HAWAII");

    @Test
    void neitherPresentIsValid() {
        ConfigurationCandidate candidate = candidate("M", "CLASSIC", Set.of());

        assertThat(evaluator.evaluate(hamRequiresPineapple, candidate, context)).isEmpty();
    }

    @Test
    void triggerWithRequiredIngredientIsValid() {
        ConfigurationCandidate candidate = candidate(
            "M", "CLASSIC", Set.of(), new ExtraSelection("HAM", 1), new ExtraSelection("PINEAPPLE", 1));

        assertThat(evaluator.evaluate(hamRequiresPineapple, candidate, context)).isEmpty();
    }

    @Test
    void triggerWithoutRequiredIngredientIsInvalid() {
        ConfigurationCandidate candidate = candidate("M", "CLASSIC", Set.of(), new ExtraSelection("HAM", 1));

        List<Violation> violations = evaluator.evaluate(hamRequiresPineapple, candidate, context);

        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).code()).isEqualTo("REQUIRED_INGREDIENT_MISSING");
    }
}
