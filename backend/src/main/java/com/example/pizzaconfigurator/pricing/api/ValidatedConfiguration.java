package com.example.pizzaconfigurator.pricing.api;

import java.util.List;

/**
 * A configuration Pricing is allowed to price — carries {@code ruleVersion}
 * as the "explicit trusted validation reference" agent.md §7.3 requires
 * ("Pricing must only process a configuration that has a successful rule
 * validation or an explicit trusted validation reference"). Pricing does
 * not call the Rules module itself; the caller (the future Configuration
 * module) is responsible for validating first and passing the resulting
 * rule version through here.
 */
public record ValidatedConfiguration(
    String pizzaCode,
    String sizeCode,
    String doughCode,
    List<PricedExtra> extras,
    String ruleVersion
) {
    public ValidatedConfiguration {
        if (ruleVersion == null || ruleVersion.isBlank()) {
            throw new IllegalArgumentException(
                "ValidatedConfiguration requires a rule validation reference (ruleVersion) — agent.md §7.3");
        }
    }
}
