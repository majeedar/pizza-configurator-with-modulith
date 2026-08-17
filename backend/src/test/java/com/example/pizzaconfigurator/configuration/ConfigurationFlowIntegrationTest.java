package com.example.pizzaconfigurator.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.pizzaconfigurator.SharedTestcontainersConfiguration;
import com.example.pizzaconfigurator.catalog.api.CatalogQuery;
import com.example.pizzaconfigurator.catalog.api.PizzaSummary;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Proves the Phase 5 Definition of Done (agent.md §32 Phase 5): a standard
 * no-comment configuration works end-to-end up to checkout, for both guest
 * and authenticated customers.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(SharedTestcontainersConfiguration.class)
@Testcontainers
class ConfigurationFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private CatalogQuery catalogQuery;

    private UUID margheritaId() {
        return catalogQuery.findActivePizzas().stream()
            .filter(p -> p.code().equals("MARGHERITA"))
            .map(PizzaSummary::pizzaId)
            .findFirst().orElseThrow();
    }

    @Test
    void standardMargheritaWorksEndToEndForAGuest() throws Exception {
        String createBody = """
            {"pizzaId":"%s","sizeCode":"M","doughCode":"CLASSIC","removedIngredients":[],"extras":[]}
            """.formatted(margheritaId());

        String createResponse = mockMvc.perform(post("/api/v1/configurations")
                .contentType(MediaType.APPLICATION_JSON).content(createBody))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        JsonNode created = jsonMapper.readTree(createResponse);
        assertThat(created.get("customerId").isNull()).isTrue();
        String configurationId = created.get("configurationId").asString();

        String validateResponse = mockMvc.perform(post("/api/v1/configurations/" + configurationId + "/validate"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        JsonNode validated = jsonMapper.readTree(validateResponse);
        assertThat(validated.get("session").get("validationStatus").asString()).isEqualTo("VALID");
        assertThat(validated.get("violations")).isEmpty();

        String priceResponse = mockMvc.perform(post("/api/v1/configurations/" + configurationId + "/price"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        JsonNode priced = jsonMapper.readTree(priceResponse);
        assertThat(priced.get("session").get("priceStatus").asString()).isEqualTo("READY_FOR_CHECKOUT");
        assertThat(priced.get("quote").get("total").decimalValue()).isEqualByComparingTo("10.00");
        assertThat(priced.get("session").get("calculatedPrice").decimalValue()).isEqualByComparingTo("10.00");
    }

    @Test
    void extraCheeseAboveMaxIsInvalidAndCannotBePriced() throws Exception {
        String createBody = """
            {"pizzaId":"%s","sizeCode":"M","doughCode":"CLASSIC","removedIngredients":[],"extras":[{"ingredientCode":"CHEESE","quantity":3}]}
            """.formatted(margheritaId());

        String created = mockMvc.perform(post("/api/v1/configurations")
                .contentType(MediaType.APPLICATION_JSON).content(createBody))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        String configurationId = jsonMapper.readTree(created).get("configurationId").asString();

        String validateResponse = mockMvc.perform(post("/api/v1/configurations/" + configurationId + "/validate"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        JsonNode validated = jsonMapper.readTree(validateResponse);
        assertThat(validated.get("session").get("validationStatus").asString()).isEqualTo("INVALID");
        assertThat(validated.get("violations").get(0).get("code").asString()).isEqualTo("MAX_QUANTITY_EXCEEDED");

        mockMvc.perform(post("/api/v1/configurations/" + configurationId + "/price"))
            .andExpect(status().isConflict());
    }

    @Test
    void nonBlankCommentIsParkedAsPendingReview() throws Exception {
        String createBody = """
            {"pizzaId":"%s","sizeCode":"M","doughCode":"CLASSIC","removedIngredients":[],"extras":[],"comment":"extra crispy please"}
            """.formatted(margheritaId());

        String created = mockMvc.perform(post("/api/v1/configurations")
                .contentType(MediaType.APPLICATION_JSON).content(createBody))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        String configurationId = jsonMapper.readTree(created).get("configurationId").asString();

        String validateResponse = mockMvc.perform(post("/api/v1/configurations/" + configurationId + "/validate"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        JsonNode validated = jsonMapper.readTree(validateResponse);
        assertThat(validated.get("session").get("validationStatus").asString()).isEqualTo("PENDING_REVIEW");
    }

    @Test
    void authenticatedCustomerIsAttachedToTheSession() throws Exception {
        String email = "dave-" + UUID.randomUUID() + "@example.com";
        String registerBody = """
            {"name":"Dave","email":"%s","phoneNumber":null,"password":"correcthorse"}
            """.formatted(email);
        String registerResponse = mockMvc.perform(post("/api/v1/customers/register")
                .contentType(MediaType.APPLICATION_JSON).content(registerBody))
            .andReturn().getResponse().getContentAsString();
        JsonNode auth = jsonMapper.readTree(registerResponse);
        String token = auth.get("token").asString();
        String customerId = auth.get("customerId").asString();

        String createBody = """
            {"pizzaId":"%s","sizeCode":"M","doughCode":"CLASSIC","removedIngredients":[],"extras":[]}
            """.formatted(margheritaId());

        String created = mockMvc.perform(post("/api/v1/configurations")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(createBody))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        assertThat(jsonMapper.readTree(created).get("customerId").asString()).isEqualTo(customerId);
    }

    @Test
    void unknownConfigurationReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/configurations/00000000-0000-0000-0000-000000000000"))
            .andExpect(status().isNotFound());
    }
}
