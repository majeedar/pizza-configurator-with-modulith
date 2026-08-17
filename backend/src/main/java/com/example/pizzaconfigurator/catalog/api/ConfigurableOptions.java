package com.example.pizzaconfigurator.catalog.api;

import java.util.List;
import java.util.UUID;

/**
 * The full set of choices a customer can make for a given pizza: its base
 * recipe (what can be removed), the extras it can be topped with, and the
 * active sizes/doughs. Rule-aware constraints (max quantities, exclusions,
 * etc.) are layered on top by the Rule Module — Catalog only reports what
 * exists, not what's valid.
 */
public record ConfigurableOptions(
    UUID pizzaId,
    String pizzaName,
    List<RecipeItemView> baseIngredients,
    List<IngredientOptionView> availableExtras,
    List<SizeOptionView> sizes,
    List<DoughOptionView> doughs
) {
}
