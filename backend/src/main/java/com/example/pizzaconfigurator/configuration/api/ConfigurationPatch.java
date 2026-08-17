package com.example.pizzaconfigurator.configuration.api;

import java.util.List;
import java.util.Set;

/**
 * A kitchen-proposed alternative configuration (agent.md §4.3/§7.11) — a
 * full replacement, not a delta, matching "Kitchen proposes an alternative
 * configuration" rather than a partial edit.
 */
public record ConfigurationPatch(
    Set<String> removedIngredientCodes,
    List<SelectionExtra> extras,
    String sizeCode,
    String doughCode
) {
}
