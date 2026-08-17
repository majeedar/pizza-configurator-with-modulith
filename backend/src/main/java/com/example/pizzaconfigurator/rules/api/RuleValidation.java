package com.example.pizzaconfigurator.rules.api;

/**
 * The business authority for configuration validity (agent.md §7.2).
 * AI may interpret a comment; only this module decides whether the
 * resulting configuration is valid.
 */
public interface RuleValidation {

    ValidationResult validate(ConfigurationCandidate candidate);
}
