package com.example.pizzaconfigurator.rules.domain;

import com.example.pizzaconfigurator.catalog.api.ConfigurableOptions;
import com.example.pizzaconfigurator.catalog.api.RecipeItemView;
import com.example.pizzaconfigurator.rules.api.ConfigurationCandidate;
import java.util.Optional;

/**
 * Catalog facts an evaluator needs (recipe, available extras/sizes/doughs).
 * Gathered once by {@code RuleValidationService} so individual evaluators
 * stay pure functions of (rule, candidate, context) — no DB/HTTP calls of
 * their own (agent.md §11).
 */
public record RuleEvaluationContext(String pizzaCode, ConfigurableOptions catalogOptions) {

    public Optional<RecipeItemView> baseIngredient(String ingredientCode) {
        return catalogOptions.baseIngredients().stream()
            .filter(item -> item.ingredientCode().equals(ingredientCode))
            .findFirst();
    }

    /**
     * True if the ingredient ends up present in the final pizza: either it
     * was added as an extra, or it's a non-removed base ingredient.
     */
    public boolean isIngredientPresent(ConfigurationCandidate candidate, String ingredientCode) {
        if (candidate.hasExtra(ingredientCode)) {
            return true;
        }
        boolean isBase = baseIngredient(ingredientCode).isPresent();
        boolean removed = candidate.removedIngredientCodes().contains(ingredientCode);
        return isBase && !removed;
    }
}
