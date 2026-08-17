package com.example.pizzaconfigurator.aiadapter.api;

import java.util.UUID;

/**
 * Agent.md §7.4: the AI Adapter receives only the free-text portion plus
 * necessary constrained context — {@code pizzaId} lets it look up the
 * allowlisted ingredient/size/dough codes itself (via {@code CatalogQuery})
 * rather than the caller assembling and forwarding that context.
 */
public record CommentInterpretationRequest(UUID pizzaId, String comment) {
}
