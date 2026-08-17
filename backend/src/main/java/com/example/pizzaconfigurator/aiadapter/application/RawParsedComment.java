package com.example.pizzaconfigurator.aiadapter.application;

import java.util.List;

/**
 * Exactly the structured-output schema from agent.md §13.2 — deserialized
 * directly from the provider's JSON response. Deliberately not trusted yet:
 * {@link CommentInterpreterService} still normalizes every code against the
 * pizza's actual allowed options before any of this reaches a domain type.
 */
record RawParsedComment(
    List<String> removeIngredients,
    List<RawExtra> extras,
    String requestedSize,
    String requestedDough,
    List<String> unresolvedText
) {
    RawParsedComment {
        removeIngredients = removeIngredients == null ? List.of() : removeIngredients;
        extras = extras == null ? List.of() : extras;
        unresolvedText = unresolvedText == null ? List.of() : unresolvedText;
    }
}
