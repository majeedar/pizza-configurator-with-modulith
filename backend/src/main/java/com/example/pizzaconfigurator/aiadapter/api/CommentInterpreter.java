package com.example.pizzaconfigurator.aiadapter.api;

/**
 * Agent.md §7.4: provider-agnostic — Deepseek primary, OpenAI fallback,
 * never hardwired to a single vendor. Never decides validity, never
 * calculates price, never creates or modifies an Order; the caller is
 * responsible for merging {@link ParsedComment} with the customer's
 * structured selections and sending the result through the Rule Module.
 */
public interface CommentInterpreter {

    ParsedComment interpret(CommentInterpretationRequest request);
}
