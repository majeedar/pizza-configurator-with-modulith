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

class ExclusionEvaluatorTest {

    private final ExclusionEvaluator evaluator = new ExclusionEvaluator(JSON);
    private final RuleDefinition anchovyPineappleExcluded = rule(
        "ANCHOVY_PINEAPPLE_EXCLUDED", RuleType.EXCLUDES,
        "{\"ingredientA\":\"ANCHOVY\",\"ingredientB\":\"PINEAPPLE\"}");
    private final RuleEvaluationContext context = context("NAPOLI");

    @Test
    void onlyOnePresentIsValid() {
        ConfigurationCandidate candidate = candidate("M", "CLASSIC", Set.of(), new ExtraSelection("ANCHOVY", 1));

        assertThat(evaluator.evaluate(anchovyPineappleExcluded, candidate, context)).isEmpty();
    }

    @Test
    void bothPresentIsInvalid() {
        ConfigurationCandidate candidate = candidate(
            "M", "CLASSIC", Set.of(), new ExtraSelection("ANCHOVY", 1), new ExtraSelection("PINEAPPLE", 1));

        List<Violation> violations = evaluator.evaluate(anchovyPineappleExcluded, candidate, context);

        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).code()).isEqualTo("INCOMPATIBLE_INGREDIENTS");
    }
}
