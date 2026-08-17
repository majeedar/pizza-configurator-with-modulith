package com.example.pizzaconfigurator.rules.api;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * A customer's proposed pizza configuration, as submitted for validation.
 * Carries no verdict of its own — only the Rule Module decides that.
 */
public record ConfigurationCandidate(
    UUID pizzaId,
    String sizeCode,
    String doughCode,
    Set<String> removedIngredientCodes,
    List<ExtraSelection> extras
) {

    public int extraQuantityOf(String ingredientCode) {
        return extras.stream()
            .filter(e -> e.ingredientCode().equals(ingredientCode))
            .mapToInt(ExtraSelection::quantity)
            .sum();
    }

    public boolean hasExtra(String ingredientCode) {
        return extraQuantityOf(ingredientCode) > 0;
    }
}
