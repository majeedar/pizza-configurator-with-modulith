package com.example.pizzaconfigurator.recommendation;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
 * Agent.md Phase 10 Definition of Done: an unresolved request can be
 * Accepted, Recommended-then-customer-Accepted,
 * Recommended-then-customer-Rejected, or Rejected — all four paths tested —
 * and no Order exists until final customer price confirmation regardless
 * of path (§4.3). Each scenario opens its own {@code ReviewRequest} via a
 * comment the (WireMock-stubbed) AI Adapter deliberately can't resolve —
 * the same trigger proven in {@code AiCommentConfigurationIntegrationTest}
 * — since that's a simpler, fully-deterministic way to reach {@code
 * PENDING_REVIEW} than hand-crafting a rule violation with no suggestion.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(SharedTestcontainersConfiguration.class)
@Testcontainers
class RecommendationFlowIntegrationTest {

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
    }

    @BeforeEach
    void resetStubs() {
        deepseek.resetAll();
        deepseek.stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/chat/completions"))
            .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                .withBody("{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":"
                    + "\"{\\\"removeIngredients\\\":[],\\\"extras\\\":[],\\\"requestedSize\\\":null,\\\"requestedDough\\\":null,"
                    + "\\\"unresolvedText\\\":[\\\"make it dance\\\"]}\"}}]}")));
    }

    private UUID margheritaId() {
        return catalogQuery.findActivePizzas().stream()
            .filter(p -> p.code().equals("MARGHERITA"))
            .map(PizzaSummary::pizzaId)
            .findFirst().orElseThrow();
    }

    /** Opens a fresh {@code ReviewRequest} (via an AI-unresolved comment) and returns {@code [configurationId, reviewRequestId]}. */
    private String[] openReviewRequest() throws Exception {
        String createBody = """
            {"pizzaId":"%s","sizeCode":"M","doughCode":"CLASSIC","removedIngredients":[],"extras":[],"comment":"Make it dance."}
            """.formatted(margheritaId());
        String created = mockMvc.perform(post("/api/v1/configurations")
                .contentType(MediaType.APPLICATION_JSON).content(createBody))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        String configurationId = jsonMapper.readTree(created).get("configurationId").asString();

        mockMvc.perform(post("/api/v1/configurations/" + configurationId + "/validate")).andExpect(status().isOk());

        String reviewStatus = mockMvc.perform(get("/api/v1/configurations/" + configurationId + "/review-status"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        String reviewRequestId = jsonMapper.readTree(reviewStatus).get("reviewRequestId").asString();

        return new String[] {configurationId, reviewRequestId};
    }

    private String kitchenToken() throws Exception {
        String response = mockMvc.perform(post("/api/v1/staff/login").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"kitchen","password":"kitchen123"}
                    """))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return jsonMapper.readTree(response).get("token").asString();
    }

    @Test
    void kitchenAcceptRevalidatesAndPricesTheOriginalConfiguration() throws Exception {
        String[] ids = openReviewRequest();
        String configurationId = ids[0];
        String reviewRequestId = ids[1];
        String kitchenToken = kitchenToken();

        String response = mockMvc.perform(post("/api/v1/kitchen/reviews/" + reviewRequestId + "/accept")
                .header("Authorization", "Bearer " + kitchenToken))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        JsonNode outcome = jsonMapper.readTree(response);

        assertThat(outcome.get("reviewRequest").get("status").asString()).isEqualTo("ACCEPTED_BY_KITCHEN");
        assertThat(outcome.get("reviewRequest").get("reviewedBy").asString()).isEqualTo("kitchen");
        assertThat(outcome.get("session").get("validationStatus").asString()).isEqualTo("REVIEW_APPROVED");
        assertThat(outcome.get("session").get("priceStatus").asString()).isEqualTo("READY_FOR_CHECKOUT");

        // Proves the flow can now go all the way to a real Order — but not before this point.
        String basketId = mockMvc.perform(post("/api/v1/baskets"))
            .andReturn().getResponse().getContentAsString();
        String basketIdValue = jsonMapper.readTree(basketId).get("basketId").asString();
        mockMvc.perform(post("/api/v1/baskets/" + basketIdValue + "/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"configurationId":"%s","quantity":1}
                    """.formatted(configurationId)))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/orders")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"basketId":"%s"}
                    """.formatted(basketIdValue)))
            .andExpect(status().isCreated());
    }

    @Test
    void kitchenRecommendThenCustomerAcceptAppliesThePatchAndPrices() throws Exception {
        String[] ids = openReviewRequest();
        String configurationId = ids[0];
        String reviewRequestId = ids[1];
        String kitchenToken = kitchenToken();

        String recommendResponse = mockMvc.perform(post("/api/v1/kitchen/reviews/" + reviewRequestId + "/recommend")
                .header("Authorization", "Bearer " + kitchenToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"removedIngredientCodes":[],"extras":[{"ingredientCode":"CHEESE","quantity":1}],"sizeCode":"L","doughCode":"CLASSIC"}
                    """))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        assertThat(jsonMapper.readTree(recommendResponse).get("reviewRequest").get("status").asString())
            .isEqualTo("RECOMMENDED_BY_KITCHEN");

        // No Order-relevant state changed yet — the ConfigurationSession still reflects the customer's original selections.
        String beforeAccept = mockMvc.perform(get("/api/v1/configurations/" + configurationId))
            .andReturn().getResponse().getContentAsString();
        assertThat(jsonMapper.readTree(beforeAccept).get("sizeCode").asString()).isEqualTo("M");

        String acceptResponse = mockMvc.perform(post("/api/v1/configurations/" + configurationId + "/recommendation/accept"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        JsonNode outcome = jsonMapper.readTree(acceptResponse);

        assertThat(outcome.get("reviewRequest").get("status").asString()).isEqualTo("RECOMMENDATION_ACCEPTED_BY_CUSTOMER");
        assertThat(outcome.get("session").get("validationStatus").asString()).isEqualTo("REVIEW_APPROVED");
        assertThat(outcome.get("session").get("sizeCode").asString()).isEqualTo("L");
        assertThat(outcome.get("session").get("priceStatus").asString()).isEqualTo("READY_FOR_CHECKOUT");
        boolean hasCheeseExtra = false;
        for (JsonNode extra : outcome.get("session").get("extras")) {
            if (extra.get("ingredientCode").asString().equals("CHEESE")) {
                hasCheeseExtra = true;
            }
        }
        assertThat(hasCheeseExtra).isTrue();
    }

    @Test
    void kitchenRecommendThenCustomerRejectEndsTheSessionWithNoOrderPossible() throws Exception {
        String[] ids = openReviewRequest();
        String configurationId = ids[0];
        String reviewRequestId = ids[1];
        String kitchenToken = kitchenToken();

        mockMvc.perform(post("/api/v1/kitchen/reviews/" + reviewRequestId + "/recommend")
                .header("Authorization", "Bearer " + kitchenToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"removedIngredientCodes":[],"extras":[],"sizeCode":"L","doughCode":"CLASSIC"}
                    """))
            .andExpect(status().isOk());

        String rejectResponse = mockMvc.perform(post("/api/v1/configurations/" + configurationId + "/recommendation/reject"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        JsonNode outcome = jsonMapper.readTree(rejectResponse);

        assertThat(outcome.get("reviewRequest").get("status").asString()).isEqualTo("RECOMMENDATION_REJECTED_BY_CUSTOMER");
        assertThat(outcome.get("session").get("validationStatus").asString()).isEqualTo("REVIEW_REJECTED");

        // No Order can exist from this session — pricing (a checkout prerequisite) is refused.
        mockMvc.perform(post("/api/v1/configurations/" + configurationId + "/price")).andExpect(status().isConflict());
    }

    @Test
    void kitchenRejectEndsTheSessionWithNoOrderPossible() throws Exception {
        String[] ids = openReviewRequest();
        String configurationId = ids[0];
        String reviewRequestId = ids[1];
        String kitchenToken = kitchenToken();

        String rejectResponse = mockMvc.perform(post("/api/v1/kitchen/reviews/" + reviewRequestId + "/reject")
                .header("Authorization", "Bearer " + kitchenToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"reason":"Out of dough for large orders tonight."}
                    """))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        JsonNode outcome = jsonMapper.readTree(rejectResponse);

        assertThat(outcome.get("reviewRequest").get("status").asString()).isEqualTo("REJECTED_BY_KITCHEN");
        assertThat(outcome.get("session").get("validationStatus").asString()).isEqualTo("REVIEW_REJECTED");

        mockMvc.perform(post("/api/v1/configurations/" + configurationId + "/price")).andExpect(status().isConflict());
    }

    @Test
    void acceptingAnAlreadyResolvedReviewRequestIsRejected() throws Exception {
        String[] ids = openReviewRequest();
        String reviewRequestId = ids[1];
        String kitchenToken = kitchenToken();

        mockMvc.perform(post("/api/v1/kitchen/reviews/" + reviewRequestId + "/accept")
                .header("Authorization", "Bearer " + kitchenToken))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/kitchen/reviews/" + reviewRequestId + "/accept")
                .header("Authorization", "Bearer " + kitchenToken))
            .andExpect(status().isConflict());
    }
}
