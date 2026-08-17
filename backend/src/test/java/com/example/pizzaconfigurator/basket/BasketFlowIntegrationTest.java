package com.example.pizzaconfigurator.basket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

@SpringBootTest
@AutoConfigureMockMvc
@Import(SharedTestcontainersConfiguration.class)
@Testcontainers
class BasketFlowIntegrationTest {

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

    private String readyToCheckoutConfigurationId() throws Exception {
        String createBody = """
            {"pizzaId":"%s","sizeCode":"M","doughCode":"CLASSIC","removedIngredients":[],"extras":[]}
            """.formatted(margheritaId());
        String created = mockMvc.perform(post("/api/v1/configurations")
                .contentType(MediaType.APPLICATION_JSON).content(createBody))
            .andReturn().getResponse().getContentAsString();
        String configurationId = jsonMapper.readTree(created).get("configurationId").asString();

        mockMvc.perform(post("/api/v1/configurations/" + configurationId + "/validate")).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/configurations/" + configurationId + "/price")).andExpect(status().isOk());
        return configurationId;
    }

    @Test
    void addingAReadyConfigurationUpdatesTheBasketTotal() throws Exception {
        String basketResponse = mockMvc.perform(post("/api/v1/baskets"))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        String basketId = jsonMapper.readTree(basketResponse).get("basketId").asString();

        String configurationId = readyToCheckoutConfigurationId();
        String addItemBody = """
            {"configurationId":"%s","quantity":2}
            """.formatted(configurationId);

        String afterAdd = mockMvc.perform(post("/api/v1/baskets/" + basketId + "/items")
                .contentType(MediaType.APPLICATION_JSON).content(addItemBody))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        JsonNode basket = jsonMapper.readTree(afterAdd);

        // Margherita M/CLASSIC = 10.00, quantity 2 => 20.00
        assertThat(basket.get("total").decimalValue()).isEqualByComparingTo("20.00");
        assertThat(basket.get("items")).hasSize(1);

        String itemId = basket.get("items").get(0).get("basketItemId").asString();
        String afterRemove = mockMvc.perform(delete("/api/v1/baskets/" + basketId + "/items/" + itemId))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        JsonNode emptied = jsonMapper.readTree(afterRemove);
        assertThat(emptied.get("total").decimalValue()).isEqualByComparingTo("0.00");
        assertThat(emptied.get("items")).isEmpty();
    }

    @Test
    void addingAnUnpricedConfigurationIsRejected() throws Exception {
        String createBody = """
            {"pizzaId":"%s","sizeCode":"M","doughCode":"CLASSIC","removedIngredients":[],"extras":[]}
            """.formatted(margheritaId());
        String created = mockMvc.perform(post("/api/v1/configurations")
                .contentType(MediaType.APPLICATION_JSON).content(createBody))
            .andReturn().getResponse().getContentAsString();
        String configurationId = jsonMapper.readTree(created).get("configurationId").asString();
        // Deliberately not validated/priced yet.

        String basketResponse = mockMvc.perform(post("/api/v1/baskets")).andReturn().getResponse().getContentAsString();
        String basketId = jsonMapper.readTree(basketResponse).get("basketId").asString();

        String addItemBody = """
            {"configurationId":"%s","quantity":1}
            """.formatted(configurationId);

        mockMvc.perform(post("/api/v1/baskets/" + basketId + "/items")
                .contentType(MediaType.APPLICATION_JSON).content(addItemBody))
            .andExpect(status().isConflict());
    }
}
