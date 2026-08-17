package com.example.pizzaconfigurator.aiadapter.application;

import java.util.List;

/**
 * Deepseek and OpenAI both expose an OpenAI-compatible Chat Completions
 * API, so one request/response shape covers both providers. {@code
 * response_format} keeps its wire-format snake_case name directly (rather
 * than a Java-style field plus a {@code @JsonProperty} annotation) to avoid
 * depending on exactly which Jackson-annotations package/version is on the
 * classpath.
 */
record ChatCompletionRequest(String model, List<ChatMessage> messages, ChatResponseFormat response_format, double temperature) {
}
