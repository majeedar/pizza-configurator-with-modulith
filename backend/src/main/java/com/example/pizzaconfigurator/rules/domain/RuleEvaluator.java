package com.example.pizzaconfigurator.rules.domain;

import com.example.pizzaconfigurator.rules.api.ConfigurationCandidate;
import com.example.pizzaconfigurator.rules.api.Violation;
import java.util.List;

public interface RuleEvaluator {

    boolean supports(RuleDefinition rule);

    List<Violation> evaluate(RuleDefinition rule, ConfigurationCandidate configuration, RuleEvaluationContext context);
}
