package com.example.pizzaconfigurator.aiadapter.api;

import java.util.List;

/**
 * Agent.md §13.2: already schema-validated and catalog-normalized — codes
 * that don't resolve against the pizza's allowed ingredients/sizes/doughs
 * are moved into {@code unresolvedText} rather than passed through, and
 * {@code requestedSizeCode}/{@code requestedDoughCode} are null unless the
 * comment actually requested (and validly resolved) a change.
 *
 * <p>Never considered "successful" merely because the provider returned
 * parseable JSON (agent.md §4.2) — the caller must still send the merged
 * candidate through the Rule Module. A non-empty {@code unresolvedText}
 * (agent.md §13.3: timeout, error, malformed response, unknown ingredient,
 * or unresolved language) means the caller must not attempt to merge/act
 * on this result at all and should route the whole configuration to manual
 * review instead — including when both providers failed outright, which
 * {@link #providerFailure()} represents as a single synthetic entry.
 */
public record ParsedComment(
    List<String> removeIngredientCodes,
    List<ParsedExtra> extras,
    String requestedSizeCode,
    String requestedDoughCode,
    List<String> unresolvedText
) {

    public static ParsedComment providerFailure() {
        return new ParsedComment(List.of(), List.of(), null, null, List.of("AI providers unavailable"));
    }

    public boolean isFullyResolved() {
        return unresolvedText.isEmpty();
    }
}
