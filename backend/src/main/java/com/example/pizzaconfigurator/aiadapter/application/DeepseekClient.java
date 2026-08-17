package com.example.pizzaconfigurator.aiadapter.application;

import com.example.pizzaconfigurator.catalog.api.ConfigurableOptions;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

/**
 * Primary provider (agent.md §7.4, ADR-008). Deepseek's Chat Completions
 * API is OpenAI-compatible.
 */
@Component
class DeepseekClient implements AiProviderClient {

    private static final int CIRCUIT_BREAKER_FAILURE_THRESHOLD = 3;
    private static final Duration CIRCUIT_BREAKER_OPEN_DURATION = Duration.ofSeconds(30);

    private final RestClient restClient;
    private final String model;
    private final String apiKey;
    private final JsonMapper jsonMapper;
    private final SimpleCircuitBreaker circuitBreaker;

    DeepseekClient(
        @Value("${app.ai.deepseek.base-url}") String baseUrl,
        @Value("${app.ai.deepseek.api-key:}") String apiKey,
        @Value("${app.ai.deepseek.model}") String model,
        @Value("${app.ai.deepseek.timeout-ms}") long timeoutMs,
        JsonMapper jsonMapper,
        Clock clock
    ) {
        this.apiKey = apiKey;
        this.model = model;
        this.jsonMapper = jsonMapper;
        this.circuitBreaker = new SimpleCircuitBreaker(CIRCUIT_BREAKER_FAILURE_THRESHOLD, CIRCUIT_BREAKER_OPEN_DURATION, clock);

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) timeoutMs);
        requestFactory.setReadTimeout((int) timeoutMs);
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    @Override
    public String name() {
        return "Deepseek";
    }

    /** Package-visible test seam only — the circuit breaker is otherwise a long-lived singleton with no reset. */
    void resetCircuitBreakerForTests() {
        circuitBreaker.reset();
    }

    @Override
    public RawParsedComment interpret(String comment, ConfigurableOptions allowedOptions) {
        if (apiKey.isBlank()) {
            throw new AiProviderException(name() + " is not configured (DEEPSEEK_API_KEY unset)");
        }
        if (!circuitBreaker.allowRequest()) {
            throw new AiProviderException(name() + " circuit breaker is open");
        }
        try {
            RawParsedComment result = call(comment, allowedOptions);
            circuitBreaker.recordSuccess();
            return result;
        } catch (RuntimeException e) {
            circuitBreaker.recordFailure();
            throw new AiProviderException(name() + " request failed: " + e.getMessage(), e);
        }
    }

    private RawParsedComment call(String comment, ConfigurableOptions allowedOptions) {
        ChatCompletionRequest request = new ChatCompletionRequest(
            model,
            List.of(
                new ChatMessage("system", CommentPromptBuilder.systemPrompt(allowedOptions)),
                new ChatMessage("user", comment)),
            ChatResponseFormat.jsonObject(),
            0.0);

        ChatCompletionResponse response = restClient.post()
            .uri("/chat/completions")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .body(ChatCompletionResponse.class);

        if (response == null || response.choices().isEmpty()) {
            throw new AiProviderException(name() + " returned no choices");
        }
        String content = response.choices().get(0).message().content();
        try {
            return jsonMapper.readValue(content, RawParsedComment.class);
        } catch (RuntimeException e) {
            throw new AiProviderException(name() + " returned malformed JSON", e);
        }
    }
}
