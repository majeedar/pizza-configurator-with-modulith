package com.example.pizzaconfigurator.configuration.domain;

import java.util.List;
import java.util.Set;

/**
 * The structurally variable part of a configuration — what's been removed
 * and what's been added — persisted as JSON in {@code configurationJson}
 * rather than normalized rows (agent.md §5.1, §6).
 */
public record SelectionSnapshot(Set<String> removedIngredientCodes, List<Extra> extras) {

    public record Extra(String ingredientCode, int quantity) {
    }
}
