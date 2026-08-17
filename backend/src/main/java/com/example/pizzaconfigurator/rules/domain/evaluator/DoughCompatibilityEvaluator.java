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

@Component
public class DoughCompatibilityEvaluator implements RuleEvaluator {

    private final JsonMapper jsonMapper;

    public DoughCompatibilityEvaluator(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public record Params(String doughCode, boolean allowed) {
    }

    @Override
    public boolean supports(RuleDefinition rule) {
        return rule.getRuleType() == RuleType.DOUGH_COMPATIBILITY;
    }

    @Override
    public List<Violation> evaluate(RuleDefinition rule, ConfigurationCandidate configuration, RuleEvaluationContext context) {
        Params params = jsonMapper.readValue(rule.getParametersJson(), Params.class);

        if (configuration.doughCode().equals(params.doughCode()) && !params.allowed()) {
            return List.of(new Violation(
                "DOUGH_NOT_AVAILABLE",
                "doughCode",
                rule.getRuleCode(),
                rule.getMessage()
            ));
        }
        return List.of();
    }
}
