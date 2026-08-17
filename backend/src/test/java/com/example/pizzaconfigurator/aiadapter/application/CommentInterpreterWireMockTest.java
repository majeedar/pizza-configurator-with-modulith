package com.example.pizzaconfigurator.aiadapter.application;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.pizzaconfigurator.SharedTestcontainersConfiguration;
import com.example.pizzaconfigurator.aiadapter.api.CommentInterpretationRequest;
import com.example.pizzaconfigurator.aiadapter.api.CommentInterpreter;
import com.example.pizzaconfigurator.aiadapter.api.ParsedComment;
import com.example.pizzaconfigurator.catalog.api.CatalogQuery;
import com.example.pizzaconfigurator.catalog.api.PizzaSummary;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Agent.md §20.5: WireMock stands in for Deepseek and OpenAI — no live AI
 * calls in ordinary tests. Covers success, timeout, 429, 5xx, malformed
 * JSON, and schema violation, plus the Deepseek→OpenAI failover path and
 * the both-fail→{@code MANUAL_REVIEW_REQUIRED} fallback (agent.md §7.4,
 * Phase 9 Definition of Done).
 *
 * <p>Lives in this package (not {@code aiadapter}) specifically to reach
 * the package-private {@code resetCircuitBreakerForTests()} test seam on
 * {@link DeepseekClient}/{@link OpenAiClient} — both clients' circuit
 * breakers are ordinary Spring-singleton state that would otherwise leak
 * across test methods sharing the cached Spring context, since several
 * tests here deliberately fail a provider several times over.
 */
@SpringBootTest
@Import(SharedTestcontainersConfiguration.class)
@Testcontainers
class CommentInterpreterWireMockTest {

    private static WireMockServer deepseek;
    private static WireMockServer openAi;

    @Autowired
    private CommentInterpreter commentInterpreter;

    @Autowired
    private CatalogQuery catalogQuery;

    @Autowired
    private DeepseekClient deepseekClient;

    @Autowired
    private OpenAiClient openAiClient;

    @BeforeAll
    static void startServers() {
        deepseek = new WireMockServer(wireMockConfig().dynamicPort());
        openAi = new WireMockServer(wireMockConfig().dynamicPort());
        deepseek.start();
        openAi.start();
    }

    @AfterAll
    static void stopServers() {
        deepseek.stop();
        openAi.stop();
    }

    @DynamicPropertySource
    static void aiProviderProperties(DynamicPropertyRegistry registry) {
        registry.add("app.ai.deepseek.base-url", () -> "http://localhost:" + deepseek.port());
        registry.add("app.ai.deepseek.api-key", () -> "test-deepseek-key");
        registry.add("app.ai.deepseek.timeout-ms", () -> "1000");
        registry.add("app.ai.openai.base-url", () -> "http://localhost:" + openAi.port());
        registry.add("app.ai.openai.api-key", () -> "test-openai-key");
        registry.add("app.ai.openai.timeout-ms", () -> "1000");
    }

    @BeforeEach
    void resetStubsAndCircuitBreakers() {
        deepseek.resetAll();
        openAi.resetAll();
        deepseekClient.resetCircuitBreakerForTests();
        openAiClient.resetCircuitBreakerForTests();
    }

    private UUID margheritaId() {
        return catalogQuery.findActivePizzas().stream()
            .filter(p -> p.code().equals("MARGHERITA"))
            .map(PizzaSummary::pizzaId)
            .findFirst().orElseThrow();
    }

    private static void stubChatCompletion(WireMockServer server, ResponseDefinitionBuilder response) {
        server.stubFor(post(urlEqualTo("/chat/completions")).willReturn(response));
    }

    private static ResponseDefinitionBuilder chatResponseWithContent(String content) {
        String escaped = content.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
        String body = "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"" + escaped + "\"}}]}";
        return aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(body);
    }

    @Test
    void deepseekSuccessIsUsedDirectlyWithoutCallingOpenAi() {
        stubChatCompletion(deepseek, chatResponseWithContent(
            "{\"removeIngredients\":[\"BASIL\"],\"extras\":[{\"ingredientCode\":\"CHEESE\",\"quantity\":2}],"
                + "\"requestedSize\":null,\"requestedDough\":null,\"unresolvedText\":[]}"));

        ParsedComment result = commentInterpreter.interpret(
            new CommentInterpretationRequest(margheritaId(), "No basil, extra cheese please."));

        assertThat(result.isFullyResolved()).isTrue();
        assertThat(result.removeIngredientCodes()).containsExactly("BASIL");
        assertThat(result.extras()).hasSize(1);
        assertThat(result.extras().get(0).ingredientCode()).isEqualTo("CHEESE");
        assertThat(result.extras().get(0).quantity()).isEqualTo(2);
        openAi.verify(0, postRequestedFor(urlEqualTo("/chat/completions")));
    }

    @Test
    void deepseekTimeoutFailsOverToOpenAiSuccess() {
        deepseek.stubFor(post(urlEqualTo("/chat/completions")).willReturn(aResponse().withFixedDelay(3000).withStatus(200)));
        stubChatCompletion(openAi, chatResponseWithContent(
            "{\"removeIngredients\":[],\"extras\":[],\"requestedSize\":\"L\",\"requestedDough\":null,\"unresolvedText\":[]}"));

        ParsedComment result = commentInterpreter.interpret(new CommentInterpretationRequest(margheritaId(), "Make it large."));

        assertThat(result.isFullyResolved()).isTrue();
        assertThat(result.requestedSizeCode()).isEqualTo("L");
    }

    @Test
    void deepseek429FailsOverToOpenAiSuccess() {
        deepseek.stubFor(post(urlEqualTo("/chat/completions"))
            .willReturn(aResponse().withStatus(429).withBody("{\"error\":\"rate limited\"}")));
        stubChatCompletion(openAi, chatResponseWithContent(
            "{\"removeIngredients\":[\"BASIL\"],\"extras\":[],\"requestedSize\":null,\"requestedDough\":null,\"unresolvedText\":[]}"));

        ParsedComment result = commentInterpreter.interpret(new CommentInterpretationRequest(margheritaId(), "No basil."));

        assertThat(result.isFullyResolved()).isTrue();
        assertThat(result.removeIngredientCodes()).containsExactly("BASIL");
    }

    @Test
    void deepseek5xxFailsOverToOpenAiSuccess() {
        deepseek.stubFor(post(urlEqualTo("/chat/completions")).willReturn(aResponse().withStatus(503)));
        stubChatCompletion(openAi, chatResponseWithContent(
            "{\"removeIngredients\":[],\"extras\":[{\"ingredientCode\":\"CHEESE\",\"quantity\":1}],"
                + "\"requestedSize\":null,\"requestedDough\":null,\"unresolvedText\":[]}"));

        ParsedComment result = commentInterpreter.interpret(new CommentInterpretationRequest(margheritaId(), "Extra cheese."));

        assertThat(result.isFullyResolved()).isTrue();
        assertThat(result.extras()).hasSize(1);
    }

    @Test
    void deepseekMalformedJsonFailsOverToOpenAiSuccess() {
        // Not valid JSON at all — the model ignored the response_format instruction.
        stubChatCompletion(deepseek, chatResponseWithContent("Sure! Removing the basil for you."));
        stubChatCompletion(openAi, chatResponseWithContent(
            "{\"removeIngredients\":[\"BASIL\"],\"extras\":[],\"requestedSize\":null,\"requestedDough\":null,\"unresolvedText\":[]}"));

        ParsedComment result = commentInterpreter.interpret(new CommentInterpretationRequest(margheritaId(), "No basil."));

        assertThat(result.isFullyResolved()).isTrue();
        assertThat(result.removeIngredientCodes()).containsExactly("BASIL");
    }

    @Test
    void deepseekSchemaViolationFailsOverToOpenAiSuccess() {
        // Valid JSON syntax, but "quantity" is a string instead of an integer.
        stubChatCompletion(deepseek, chatResponseWithContent(
            "{\"removeIngredients\":[],\"extras\":[{\"ingredientCode\":\"CHEESE\",\"quantity\":\"two\"}],"
                + "\"requestedSize\":null,\"requestedDough\":null,\"unresolvedText\":[]}"));
        stubChatCompletion(openAi, chatResponseWithContent(
            "{\"removeIngredients\":[],\"extras\":[{\"ingredientCode\":\"CHEESE\",\"quantity\":2}],"
                + "\"requestedSize\":null,\"requestedDough\":null,\"unresolvedText\":[]}"));

        ParsedComment result = commentInterpreter.interpret(new CommentInterpretationRequest(margheritaId(), "Double cheese."));

        assertThat(result.isFullyResolved()).isTrue();
        assertThat(result.extras().get(0).quantity()).isEqualTo(2);
    }

    @Test
    void openAiOwnMalformedResponseIsHandledWhenItsTheLastProvider() {
        deepseek.stubFor(post(urlEqualTo("/chat/completions")).willReturn(aResponse().withStatus(500)));
        stubChatCompletion(openAi, chatResponseWithContent("not json at all"));

        ParsedComment result = commentInterpreter.interpret(new CommentInterpretationRequest(margheritaId(), "No basil."));

        assertThat(result.isFullyResolved()).isFalse();
        assertThat(result.unresolvedText()).contains("AI providers unavailable");
    }

    @Test
    void bothProvidersFailingFallsBackToManualReviewEquivalent() {
        deepseek.stubFor(post(urlEqualTo("/chat/completions")).willReturn(aResponse().withStatus(500)));
        openAi.stubFor(post(urlEqualTo("/chat/completions")).willReturn(aResponse().withStatus(500)));

        ParsedComment result = commentInterpreter.interpret(new CommentInterpretationRequest(margheritaId(), "No basil."));

        assertThat(result.isFullyResolved()).isFalse();
        assertThat(result.removeIngredientCodes()).isEmpty();
        assertThat(result.extras()).isEmpty();
    }

    @Test
    void unknownIngredientCodeIsMovedToUnresolvedText() {
        stubChatCompletion(deepseek, chatResponseWithContent(
            "{\"removeIngredients\":[],\"extras\":[{\"ingredientCode\":\"PEPPERONI\",\"quantity\":1}],"
                + "\"requestedSize\":null,\"requestedDough\":null,\"unresolvedText\":[]}"));

        ParsedComment result = commentInterpreter.interpret(new CommentInterpretationRequest(margheritaId(), "Add pepperoni."));

        assertThat(result.isFullyResolved()).isFalse();
        assertThat(result.extras()).isEmpty();
        assertThat(result.unresolvedText()).anyMatch(text -> text.contains("PEPPERONI"));
    }
}
