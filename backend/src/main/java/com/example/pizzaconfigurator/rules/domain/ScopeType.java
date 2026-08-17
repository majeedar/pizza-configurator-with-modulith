package com.example.pizzaconfigurator.rules.domain;

/**
 * GLOBAL rules apply to every pizza. PIZZA rules apply only when
 * {@code scopeId} matches the candidate's pizza code.
 */
public enum ScopeType {
    GLOBAL,
    PIZZA
}
