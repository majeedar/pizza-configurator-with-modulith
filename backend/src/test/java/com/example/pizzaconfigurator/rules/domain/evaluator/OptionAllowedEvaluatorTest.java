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

class OptionAllowedEvaluatorTest {

    private final OptionAllowedEvaluator evaluator = new OptionAllowedEvaluator(JSON);
    private final RuleDefinition anchovyNotAllowed = rule(
        "ANCHOVY_NOT_ALLOWED", RuleType.OPTION_ALLOWED, "{\"ingredientCode\":\"ANCHOVY\",\"allowed\":false}");

    @Test
    void supportsOnlyOptionAllowedRules() {
        assertThat(evaluator.supports(anchovyNotAllowed)).isTrue();
        assertThat(evaluator.supports(rule("X", RuleType.EXCLUDES, "{}"))).isFalse();
    }

    @Test
    void notSelectedIsValid() {
        RuleEvaluationContext context = context("MARGHERITA");
        ConfigurationCandidate candidate = candidate("M", "CLASSIC", Set.of());

        assertThat(evaluator.evaluate(anchovyNotAllowed, candidate, context)).isEmpty();
    }

    @Test
    void selectedAsAnExtraIsInvalid() {
        RuleEvaluationContext context = context("MARGHERITA");
        ConfigurationCandidate candidate = candidate("M", "CLASSIC", Set.of(), new ExtraSelection("ANCHOVY", 1));

        List<Violation> violations = evaluator.evaluate(anchovyNotAllowed, candidate, context);

        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).code()).isEqualTo("OPTION_NOT_ALLOWED");
        assertThat(violations.get(0).field()).isEqualTo("extras.ANCHOVY");
    }

    @Test
    void presentAsANonRemovedBaseIngredientIsAlsoInvalid() {
        // A ban applies whether the ingredient arrived as an extra or as an
        // un-removed base ingredient (RuleEvaluationContext#isIngredientPresent
        // covers both) — this is what distinguishes it from a plain hasExtra() check.
        RuleEvaluationContext context = context("NAPOLI", base("ANCHOVY", true));
        ConfigurationCandidate candidate = candidate("M", "CLASSIC", Set.of());

        List<Violation> violations = evaluator.evaluate(anchovyNotAllowed, candidate, context);

        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).code()).isEqualTo("OPTION_NOT_ALLOWED");
    }

    @Test
    void removedBaseIngredientIsValid() {
        RuleEvaluationContext context = context("NAPOLI", base("ANCHOVY", true));
        ConfigurationCandidate candidate = candidate("M", "CLASSIC", Set.of("ANCHOVY"));

        assertThat(evaluator.evaluate(anchovyNotAllowed, candidate, context)).isEmpty();
    }

    @Test
    void allowedRuleNeverProducesAViolation() {
        RuleDefinition anchovyAllowed = rule(
            "ANCHOVY_ALLOWED", RuleType.OPTION_ALLOWED, "{\"ingredientCode\":\"ANCHOVY\",\"allowed\":true}");
        RuleEvaluationContext context = context("MARGHERITA");
        ConfigurationCandidate candidate = candidate("M", "CLASSIC", Set.of(), new ExtraSelection("ANCHOVY", 1));

        assertThat(evaluator.evaluate(anchovyAllowed, candidate, context)).isEmpty();
    }
}
