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

class MaxQuantityEvaluatorTest {

    private final MaxQuantityEvaluator evaluator = new MaxQuantityEvaluator(JSON);
    private final RuleDefinition cheeseMax2 = rule("CHEESE_MAX_2", RuleType.MAX_QUANTITY, "{\"ingredientCode\":\"CHEESE\",\"max\":2}");
    private final RuleEvaluationContext context = context("MARGHERITA");

    @Test
    void supportsOnlyMaxQuantityRules() {
        assertThat(evaluator.supports(cheeseMax2)).isTrue();
        assertThat(evaluator.supports(rule("X", RuleType.EXCLUDES, "{}"))).isFalse();
    }

    @Test
    void withinLimitIsValid() {
        ConfigurationCandidate candidate = candidate("M", "CLASSIC", Set.of(), new ExtraSelection("CHEESE", 2));

        List<Violation> violations = evaluator.evaluate(cheeseMax2, candidate, context);

        assertThat(violations).isEmpty();
    }

    @Test
    void aboveLimitIsInvalid() {
        ConfigurationCandidate candidate = candidate("M", "CLASSIC", Set.of(), new ExtraSelection("CHEESE", 3));

        List<Violation> violations = evaluator.evaluate(cheeseMax2, candidate, context);

        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).code()).isEqualTo("MAX_QUANTITY_EXCEEDED");
        assertThat(violations.get(0).field()).isEqualTo("extras.CHEESE");
        assertThat(violations.get(0).ruleCode()).isEqualTo("CHEESE_MAX_2");
    }
}
