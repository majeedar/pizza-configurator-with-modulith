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
public class SizeCompatibilityEvaluator implements RuleEvaluator {

    private final JsonMapper jsonMapper;

    public SizeCompatibilityEvaluator(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public record Params(String sizeCode, boolean allowed) {
    }

    @Override
    public boolean supports(RuleDefinition rule) {
        return rule.getRuleType() == RuleType.SIZE_COMPATIBILITY;
    }

    @Override
    public List<Violation> evaluate(RuleDefinition rule, ConfigurationCandidate configuration, RuleEvaluationContext context) {
        Params params = jsonMapper.readValue(rule.getParametersJson(), Params.class);

        if (configuration.sizeCode().equals(params.sizeCode()) && !params.allowed()) {
            return List.of(new Violation(
                "SIZE_NOT_AVAILABLE",
                "sizeCode",
                rule.getRuleCode(),
                rule.getMessage()
            ));
        }
        return List.of();
    }
}
