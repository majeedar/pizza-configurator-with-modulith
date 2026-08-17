package com.example.pizzaconfigurator.configuration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.pizzaconfigurator.SharedTestcontainersConfiguration;
import com.example.pizzaconfigurator.catalog.api.CatalogQuery;
import com.example.pizzaconfigurator.catalog.api.PizzaSummary;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Agent.md Phase 9 Definition of Done, exercised through the real
 * {@code POST /api/v1/configurations/{id}/validate} endpoint rather than
 * calling the AI Adapter directly (that's {@code CommentInterpreterWireMockTest}):
 * AI is never called for a blank comment, and a successfully-resolved
 * comment is merged with the structured UI selections before reaching the
 * Rule Module — never trusted just because it parsed.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(SharedTestcontainersConfiguration.class)
@Testcontainers
class AiCommentConfigurationIntegrationTest {

    private static WireMockServer deepseek;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private CatalogQuery catalogQuery;

    @BeforeAll
    static void startServer() {
        deepseek = new WireMockServer(wireMockConfig().dynamicPort());
        deepseek.start();
    }

    @AfterAll
    static void stopServer() {
        deepseek.stop();
    }

    @DynamicPropertySource
    static void aiProviderProperties(DynamicPropertyRegistry registry) {
        registry.add("app.ai.deepseek.base-url", () -> "http://localhost:" + deepseek.port());
        registry.add("app.ai.deepseek.api-key", () -> "test-deepseek-key");
        // OpenAI api-key left unset deliberately — these tests only need Deepseek to succeed.
    }

    @BeforeEach
    void resetStubs() {
        deepseek.resetAll();
    }

    private UUID margheritaId() {
        return catalogQuery.findActivePizzas().stream()
            .filter(p -> p.code().equals("MARGHERITA"))
            .map(PizzaSummary::pizzaId)
            .findFirst().orElseThrow();
    }

    @Test
    void blankCommentNeverCallsTheAiAdapter() throws Exception {
        String createBody = """
            {"pizzaId":"%s","sizeCode":"M","doughCode":"CLASSIC","removedIngredients":[],"extras":[]}
            """.formatted(margheritaId());
        String created = mockMvc.perform(post("/api/v1/configurations")
                .contentType(MediaType.APPLICATION_JSON).content(createBody))
            .andReturn().getResponse().getContentAsString();
        String configurationId = jsonMapper.readTree(created).get("configurationId").asString();

        mockMvc.perform(post("/api/v1/configurations/" + configurationId + "/validate")).andExpect(status().isOk());

        deepseek.verify(0, postRequestedFor(urlEqualTo("/chat/completions")));
    }

    @Test
    void resolvedCommentMergesWithUiSelectionsAndReachesTheRuleModule() throws Exception {
        deepseek.stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/chat/completions")).willReturn(aResponse().withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody("{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":"
                + "\"{\\\"removeIngredients\\\":[\\\"BASIL\\\"],\\\"extras\\\":[{\\\"ingredientCode\\\":\\\"CHEESE\\\",\\\"quantity\\\":1}],"
                + "\\\"requestedSize\\\":null,\\\"requestedDough\\\":null,\\\"unresolvedText\\\":[]}\"}}]}")));

        String createBody = """
            {"pizzaId":"%s","sizeCode":"M","doughCode":"CLASSIC","removedIngredients":[],"extras":[],"comment":"No basil, extra cheese."}
            """.formatted(margheritaId());
        String created = mockMvc.perform(post("/api/v1/configurations")
                .contentType(MediaType.APPLICATION_JSON).content(createBody))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        String configurationId = jsonMapper.readTree(created).get("configurationId").asString();

        String validateResponse = mockMvc.perform(post("/api/v1/configurations/" + configurationId + "/validate"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        JsonNode session = jsonMapper.readTree(validateResponse).get("session");

        assertThat(session.get("validationStatus").asString()).isEqualTo("VALID");
        assertThat(session.get("removedIngredientCodes")).anyMatch(n -> n.asString().equals("BASIL"));
        boolean hasCheeseExtra = false;
        for (JsonNode extra : session.get("extras")) {
            if (extra.get("ingredientCode").asString().equals("CHEESE") && extra.get("quantity").asInt() == 1) {
                hasCheeseExtra = true;
            }
        }
        assertThat(hasCheeseExtra).isTrue();

        // The merged config is now priceable — proves it was actually
        // persisted onto the session, not just used for this one call.
        mockMvc.perform(post("/api/v1/configurations/" + configurationId + "/price")).andExpect(status().isOk());
    }

    @Test
    void unresolvableCommentIsParkedAsPendingReviewRatherThanGuessed() throws Exception {
        deepseek.stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/chat/completions")).willReturn(aResponse().withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody("{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":"
                + "\"{\\\"removeIngredients\\\":[],\\\"extras\\\":[],\\\"requestedSize\\\":null,\\\"requestedDough\\\":null,"
                + "\\\"unresolvedText\\\":[\\\"make it dance\\\"]}\"}}]}")));

        String createBody = """
            {"pizzaId":"%s","sizeCode":"M","doughCode":"CLASSIC","removedIngredients":[],"extras":[],"comment":"Make it dance."}
            """.formatted(margheritaId());
        String created = mockMvc.perform(post("/api/v1/configurations")
                .contentType(MediaType.APPLICATION_JSON).content(createBody))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        String configurationId = jsonMapper.readTree(created).get("configurationId").asString();

        String validateResponse = mockMvc.perform(post("/api/v1/configurations/" + configurationId + "/validate"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        assertThat(jsonMapper.readTree(validateResponse).get("session").get("validationStatus").asString())
            .isEqualTo("PENDING_REVIEW");
    }
}
