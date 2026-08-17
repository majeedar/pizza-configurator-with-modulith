package com.example.pizzaconfigurator.rules.application;

import com.example.pizzaconfigurator.catalog.api.CatalogQuery;
import com.example.pizzaconfigurator.catalog.api.ConfigurableOptions;
import com.example.pizzaconfigurator.catalog.api.PizzaView;
import com.example.pizzaconfigurator.rules.api.ConfigurationCandidate;
import com.example.pizzaconfigurator.rules.api.ConfigurationSuggestion;
import com.example.pizzaconfigurator.rules.api.RuleValidation;
import com.example.pizzaconfigurator.rules.api.ValidationResult;
import com.example.pizzaconfigurator.rules.api.ValidationStatus;
import com.example.pizzaconfigurator.rules.api.Violation;
import com.example.pizzaconfigurator.rules.domain.RuleDefinition;
import com.example.pizzaconfigurator.rules.domain.RuleEvaluationContext;
import com.example.pizzaconfigurator.rules.domain.RuleEvaluator;
import com.example.pizzaconfigurator.rules.domain.evaluator.MaxQuantityEvaluator;
import com.example.pizzaconfigurator.rules.infrastructure.persistence.RuleRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * Determines applicable rules, runs their evaluators, aggregates violations,
 * creates deterministic suggestions where supported, and returns a complete
 * {@link ValidationResult} (agent.md §11). Does not duplicate Catalog or
 * Pricing logic.
 */
@Service
@Transactional(readOnly = true)
class RuleValidationService implements RuleValidation {

    private static final Logger log = LoggerFactory.getLogger(RuleValidationService.class);

    private final RuleRepository rules;
    private final CatalogQuery catalogQuery;
    private final List<RuleEvaluator> evaluators;
    private final JsonMapper jsonMapper;
    private final Clock clock;

    RuleValidationService(
        RuleRepository rules,
        CatalogQuery catalogQuery,
        List<RuleEvaluator> evaluators,
        JsonMapper jsonMapper,
        Clock clock
    ) {
        this.rules = rules;
        this.catalogQuery = catalogQuery;
        this.evaluators = evaluators;
        this.jsonMapper = jsonMapper;
        this.clock = clock;
    }

    @Override
    public ValidationResult validate(ConfigurationCandidate candidate) {
        PizzaView pizza = catalogQuery.getPizza(candidate.pizzaId());
        ConfigurableOptions options = catalogQuery.getOptions(candidate.pizzaId());
        RuleEvaluationContext context = new RuleEvaluationContext(pizza.code(), options);

        Instant now = Instant.now(clock);
        List<RuleDefinition> applicableRules = rules.findByActiveTrue().stream()
            .filter(rule -> rule.appliesTo(pizza.code()))
            .filter(rule -> rule.isCurrentlyValid(now))
            .sorted(Comparator.comparing(RuleDefinition::getRuleCode))
            .toList();

        List<Violation> violations = new ArrayList<>();
        for (RuleDefinition rule : applicableRules) {
            for (RuleEvaluator evaluator : evaluators) {
                if (!evaluator.supports(rule)) {
                    continue;
                }
                // A single misconfigured admin-entered rule (e.g. malformed or
                // incomplete parametersJson) must never take down validation for
                // every other pizza on the menu — skip and log it rather than
                // letting the exception propagate out of this request. Found via
                // real Admin Portal testing: a rule saved without every required
                // parameter field 500'd every subsequent /validate call for as
                // long as that GLOBAL-scope rule stayed active.
                try {
                    violations.addAll(evaluator.evaluate(rule, candidate, context));
                } catch (RuntimeException e) {
                    log.error("Rule '{}' ({}) failed to evaluate — skipping it for this validation. "
                        + "Check its parameters via the Admin Portal.", rule.getRuleCode(), rule.getRuleType(), e);
                }
            }
        }

        ValidationStatus status = violations.isEmpty() ? ValidationStatus.VALID : ValidationStatus.INVALID;
        List<ConfigurationSuggestion> suggestions = buildSuggestions(applicableRules, violations);
        String ruleVersion = computeRuleVersion(applicableRules);

        return new ValidationResult(status, ruleVersion, violations, suggestions);
    }

    /**
     * Only MAX_QUANTITY_EXCEEDED violations get a deterministic suggestion
     * for now (cap at the allowed maximum) — matches the example in
     * agent.md §10. Other violation types are left to manual/kitchen review
     * (agent.md §4.3) rather than guessed at automatically.
     */
    private List<ConfigurationSuggestion> buildSuggestions(List<RuleDefinition> applicableRules, List<Violation> violations) {
        List<ConfigurationSuggestion> suggestions = new ArrayList<>();
        for (Violation violation : violations) {
            if (!"MAX_QUANTITY_EXCEEDED".equals(violation.code())) {
                continue;
            }
            applicableRules.stream()
                .filter(rule -> rule.getRuleCode().equals(violation.ruleCode()))
                .findFirst()
                .ifPresent(rule -> {
                    MaxQuantityEvaluator.Params params = jsonMapper.readValue(rule.getParametersJson(), MaxQuantityEvaluator.Params.class);
                    String field = "extras." + params.ingredientCode();
                    suggestions.add(new ConfigurationSuggestion(
                        "Use at most " + params.max() + " portion(s) of " + params.ingredientCode() + ".",
                        Map.of(field, params.max())
                    ));
                });
        }
        return suggestions;
    }

    /**
     * Changes whenever any rule considered for this validation is created,
     * updated, or deactivated (agent.md §11.1) — each RuleDefinition's own
     * optimistic-lock {@code version} feeds this aggregate signature.
     */
    private String computeRuleVersion(List<RuleDefinition> applicableRules) {
        String signature = applicableRules.stream()
            .map(rule -> rule.getRuleCode() + ":" + rule.getVersion())
            .reduce((a, b) -> a + ";" + b)
            .orElse("");
        return Integer.toHexString(signature.hashCode());
    }
}
