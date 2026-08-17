package com.example.pizzaconfigurator.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

import com.example.pizzaconfigurator.SharedTestcontainersConfiguration;
import java.math.BigDecimal;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end check of the Phase 2 Definition of Done (agent.md §32 Phase 2):
 * the catalog read API serves Margherita/Hawaii/Napoli and their
 * configurable options, and the admin API can create a new pizza.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(SharedTestcontainersConfiguration.class)
@Testcontainers
class CatalogApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void demoDataIsSeededAndReadableByCustomers() throws Exception {
        String body = mockMvc.perform(get("/api/v1/catalog/pizzas"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        JsonNode pizzas = jsonMapper.readTree(body);

        assertThat(pizzas.isArray()).isTrue();
        var codes = StreamSupport.stream(pizzas.spliterator(), false)
            .map(p -> p.get("code").asText())
            .toList();
        assertThat(codes).containsExactlyInAnyOrder("MARGHERITA", "HAWAII", "NAPOLI");

        JsonNode margherita = StreamSupport.stream(pizzas.spliterator(), false)
            .filter(p -> p.get("code").asText().equals("MARGHERITA"))
            .findFirst().orElseThrow();
        String pizzaId = margherita.get("pizzaId").asText();

        mockMvc.perform(get("/api/v1/catalog/pizzas/" + pizzaId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.recipe", hasSize(3)));

        mockMvc.perform(get("/api/v1/catalog/pizzas/" + pizzaId + "/options"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.baseIngredients", hasSize(3)))
            .andExpect(jsonPath("$.availableExtras", hasSize(greaterThanOrEqualTo(9))))
            .andExpect(jsonPath("$.sizes", hasSize(3)))
            .andExpect(jsonPath("$.doughs", hasSize(2)));
    }

    @Test
    void unknownPizzaReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/pizzas/00000000-0000-0000-0000-000000000000"))
            .andExpect(status().isNotFound());
    }

    @Test
    void adminCanCreateAndReadBackAPizza() throws Exception {
        String requestBody = """
            {
              "code": "QUATTRO_STAGIONI",
              "name": "Quattro Stagioni",
              "description": "Four seasons",
              "basePrice": 10.50,
              "active": true
            }
            """;
        String adminToken = adminToken();

        String createBody = mockMvc.perform(post("/api/v1/admin/pizzas")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        JsonNode created = jsonMapper.readTree(createBody);

        assertThat(created.get("basePrice").decimalValue()).isEqualByComparingTo(new BigDecimal("10.50"));
        String pizzaId = created.get("pizzaId").asText();

        mockMvc.perform(get("/api/v1/admin/pizzas/" + pizzaId).header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("QUATTRO_STAGIONI"));
    }

    @Test
    void adminEndpointsRejectRequestsWithoutAStaffToken() throws Exception {
        mockMvc.perform(get("/api/v1/admin/pizzas")).andExpect(status().isUnauthorized());
    }

    /**
     * Regression test for a real bug found via Admin Portal testing (Phase 14):
     * with {@code spring.jpa.open-in-view: false}, {@code PizzaIngredientRepository
     * #findByPizza_PizzaId} returning lazy {@code Ingredient} proxies caused a
     * {@code LazyInitializationException} the moment the controller mapped them to
     * a response DTO outside the transaction. Fixed with a {@code JOIN FETCH}
     * query.
     */
    @Test
    void adminCanReadAPizzasRecipeWithoutALazyInitializationException() throws Exception {
        String adminToken = adminToken();
        String pizzaId = findMargheritaId();

        mockMvc.perform(get("/api/v1/admin/pizzas/" + pizzaId + "/recipe")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(3)))
            .andExpect(jsonPath("$[0].ingredientName").exists());
    }

    /**
     * Regression test for a real bug found via Admin Portal testing (Phase 14):
     * adding an ingredient already on a pizza's recipe violated
     * {@code pizza_ingredient_pizza_id_ingredient_id_key} and surfaced as a raw
     * 500 instead of a clean conflict response.
     */
    @Test
    void addingAnIngredientAlreadyOnTheRecipeIsAConflictNotA500() throws Exception {
        String adminToken = adminToken();
        String pizzaId = findMargheritaId();

        String recipeBody = mockMvc.perform(get("/api/v1/admin/pizzas/" + pizzaId + "/recipe")
                .header("Authorization", "Bearer " + adminToken))
            .andReturn().getResponse().getContentAsString();
        String existingIngredientCode = jsonMapper.readTree(recipeBody).get(0).get("ingredientCode").asText();

        mockMvc.perform(post("/api/v1/admin/pizzas/" + pizzaId + "/recipe")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"ingredientCode":"%s","defaultQuantity":1,"removable":true}
                    """.formatted(existingIngredientCode)))
            .andExpect(status().isConflict());
    }

    private String findMargheritaId() throws Exception {
        String body = mockMvc.perform(get("/api/v1/catalog/pizzas")).andReturn().getResponse().getContentAsString();
        JsonNode pizzas = jsonMapper.readTree(body);
        return StreamSupport.stream(pizzas.spliterator(), false)
            .filter(p -> p.get("code").asText().equals("MARGHERITA"))
            .findFirst().orElseThrow()
            .get("pizzaId").asText();
    }

    private String adminToken() throws Exception {
        String response = mockMvc.perform(post("/api/v1/staff/login").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"admin","password":"admin123"}
                    """))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return jsonMapper.readTree(response).get("token").asText();
    }
}
