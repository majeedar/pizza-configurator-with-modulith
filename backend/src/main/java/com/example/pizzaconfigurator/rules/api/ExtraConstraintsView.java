package com.example.pizzaconfigurator.rules.api;

import java.util.Map;
import java.util.Set;

/**
 * Customer-facing UI constraints derived from active MAX_QUANTITY and
 * OPTION_ALLOWED(allowed=false) rules for a pizza — lets the configurator
 * page hide/cap options before submission, rather than only rejecting them
 * server-side after the fact. Never the sole enforcement: {@link
 * RuleValidation#validate} remains authoritative on submit.
 */
public record ExtraConstraintsView(
    Map<String, Integer> maxQuantityByIngredientCode,
    Set<String> disallowedIngredientCodes
) {
}
