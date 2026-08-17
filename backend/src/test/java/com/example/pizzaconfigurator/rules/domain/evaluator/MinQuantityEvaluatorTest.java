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

class MinQuantityEvaluatorTest {

    private final MinQuantityEvaluator evaluator = new MinQuantityEvaluator(JSON);
    private final RuleDefinition cheeseMin2 = rule("CHEESE_MIN_2", RuleType.MIN_QUANTITY, "{\"ingredientCode\":\"CHEESE\",\"min\":2}");
    private final RuleEvaluationContext context = context("MARGHERITA");

    @Test
    void supportsOnlyMinQuantityRules() {
        assertThat(evaluator.supports(cheeseMin2)).isTrue();
        assertThat(evaluator.supports(rule("X", RuleType.MAX_QUANTITY, "{}"))).isFalse();
    }

    @Test
    void notSelectedAtAllIsValid() {
        ConfigurationCandidate candidate = candidate("M", "CLASSIC", Set.of());

        List<Violation> violations = evaluator.evaluate(cheeseMin2, candidate, context);

        assertThat(violations).isEmpty();
    }

    @Test
    void atOrAboveTheFloorIsValid() {
        ConfigurationCandidate candidate = candidate("M", "CLASSIC", Set.of(), new ExtraSelection("CHEESE", 2));

        List<Violation> violations = evaluator.evaluate(cheeseMin2, candidate, context);

        assertThat(violations).isEmpty();
    }

    @Test
    void selectedButBelowTheFloorIsInvalid() {
        ConfigurationCandidate candidate = candidate("M", "CLASSIC", Set.of(), new ExtraSelection("CHEESE", 1));

        List<Violation> violations = evaluator.evaluate(cheeseMin2, candidate, context);

        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).code()).isEqualTo("MIN_QUANTITY_NOT_MET");
        assertThat(violations.get(0).field()).isEqualTo("extras.CHEESE");
        assertThat(violations.get(0).ruleCode()).isEqualTo("CHEESE_MIN_2");
    }
}
