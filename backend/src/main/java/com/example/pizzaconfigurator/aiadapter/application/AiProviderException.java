package com.example.pizzaconfigurator.aiadapter.application;

/**
 * Raised by an {@link AiProviderClient} on timeout, HTTP error, malformed
 * response, or schema violation (agent.md §7.4) — always caught by {@link
 * CommentInterpreterService}, which fails over to the next provider or, if
 * none remain, falls back to {@code MANUAL_REVIEW_REQUIRED}.
 */
class AiProviderException extends RuntimeException {

    AiProviderException(String message) {
        super(message);
    }

    AiProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
