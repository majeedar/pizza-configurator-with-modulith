package com.example.pizzaconfigurator.kitchen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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

@SpringBootTest
@AutoConfigureMockMvc
@Import(SharedTestcontainersConfiguration.class)
@Testcontainers
class KitchenFlowIntegrationTest {

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

    private String placeConfirmedOrder() throws Exception {
        String createBody = """
            {"pizzaId":"%s","sizeCode":"M","doughCode":"CLASSIC","removedIngredients":[],"extras":[]}
            """.formatted(margheritaId());
        String created = mockMvc.perform(post("/api/v1/configurations")
                .contentType(MediaType.APPLICATION_JSON).content(createBody))
            .andReturn().getResponse().getContentAsString();
        String configurationId = jsonMapper.readTree(created).get("configurationId").asString();
        mockMvc.perform(post("/api/v1/configurations/" + configurationId + "/validate")).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/configurations/" + configurationId + "/price")).andExpect(status().isOk());

        String basketResponse = mockMvc.perform(post("/api/v1/baskets")).andReturn().getResponse().getContentAsString();
        String basketId = jsonMapper.readTree(basketResponse).get("basketId").asString();
        mockMvc.perform(post("/api/v1/baskets/" + basketId + "/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"configurationId":"%s","quantity":1}
                    """.formatted(configurationId)))
            .andExpect(status().isOk());

        String orderResponse = mockMvc.perform(post("/api/v1/orders")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"basketId":"%s"}
                    """.formatted(basketId)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return jsonMapper.readTree(orderResponse).get("order").get("orderId").asString();
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
    void newOrderAppearsOnTheProductionBoardAndCanReachReady() throws Exception {
        String orderId = placeConfirmedOrder();
        String token = kitchenToken();

        String boardResponse = mockMvc.perform(get("/api/v1/kitchen/orders").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        JsonNode board = jsonMapper.readTree(boardResponse);
        boolean onBoard = false;
        for (JsonNode entry : board) {
            if (entry.get("orderId").asString().equals(orderId)) {
                onBoard = true;
            }
        }
        assertThat(onBoard).isTrue();

        mockMvc.perform(post("/api/v1/kitchen/orders/" + orderId + "/approve").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APPROVED"));
        mockMvc.perform(post("/api/v1/kitchen/orders/" + orderId + "/start").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("IN_PROCESSING"));
        mockMvc.perform(post("/api/v1/kitchen/orders/" + orderId + "/ready").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("READY"));
        mockMvc.perform(post("/api/v1/kitchen/orders/" + orderId + "/complete").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED"));

        String boardAfter = mockMvc.perform(get("/api/v1/kitchen/orders").header("Authorization", "Bearer " + token))
            .andReturn().getResponse().getContentAsString();
        for (JsonNode entry : jsonMapper.readTree(boardAfter)) {
            assertThat(entry.get("orderId").asString()).isNotEqualTo(orderId);
        }
    }

    @Test
    void outOfOrderTransitionIsRejected() throws Exception {
        String orderId = placeConfirmedOrder();
        String token = kitchenToken();

        // Order is CONFIRMED — "ready" is only valid from IN_PROCESSING.
        mockMvc.perform(post("/api/v1/kitchen/orders/" + orderId + "/ready").header("Authorization", "Bearer " + token))
            .andExpect(status().isConflict());
    }
}
