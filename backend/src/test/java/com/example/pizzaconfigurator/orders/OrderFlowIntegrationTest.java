package com.example.pizzaconfigurator.orders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.pizzaconfigurator.SharedTestcontainersConfiguration;
import com.example.pizzaconfigurator.catalog.api.CatalogQuery;
import com.example.pizzaconfigurator.catalog.api.PizzaSummary;
import com.example.pizzaconfigurator.orders.api.OrderQuery;
import com.example.pizzaconfigurator.orders.api.OrderView;
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
class OrderFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private CatalogQuery catalogQuery;

    @Autowired
    private OrderQuery orderQuery;

    private UUID margheritaId() {
        return catalogQuery.findActivePizzas().stream()
            .filter(p -> p.code().equals("MARGHERITA"))
            .map(PizzaSummary::pizzaId)
            .findFirst().orElseThrow();
    }

    private String readyToCheckoutBasketId() throws Exception {
        String createBody = """
            {"pizzaId":"%s","sizeCode":"M","doughCode":"CLASSIC","removedIngredients":[],"extras":[]}
            """.formatted(margheritaId());
        String created = mockMvc.perform(post("/api/v1/configurations")
                .contentType(MediaType.APPLICATION_JSON).content(createBody))
            .andReturn().getResponse().getContentAsString();
        String configurationId = jsonMapper.readTree(created).get("configurationId").asString();
        mockMvc.perform(post("/api/v1/configurations/" + configurationId + "/validate")).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/configurations/" + configurationId + "/price")).andExpect(status().isOk());

        String basketResponse = mockMvc.perform(post("/api/v1/baskets"))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        String basketId = jsonMapper.readTree(basketResponse).get("basketId").asString();

        String addItemBody = """
            {"configurationId":"%s","quantity":1}
            """.formatted(configurationId);
        mockMvc.perform(post("/api/v1/baskets/" + basketId + "/items")
                .contentType(MediaType.APPLICATION_JSON).content(addItemBody))
            .andExpect(status().isOk());
        return basketId;
    }

    @Test
    void guestCheckoutCreatesOrderOnceAndAppearsInKitchenQuery() throws Exception {
        String basketId = readyToCheckoutBasketId();
        String idempotencyKey = UUID.randomUUID().toString();
        String requestBody = """
            {"basketId":"%s","customNotes":"Ring the bell twice"}
            """.formatted(basketId);

        String firstResponse = mockMvc.perform(post("/api/v1/orders")
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON).content(requestBody))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        JsonNode first = jsonMapper.readTree(firstResponse);
        String orderId = first.get("order").get("orderId").asString();
        String displayNumber = first.get("order").get("displayNumber").asString();
        String accessToken = first.get("accessToken").asString();
        assertThat(first.get("order").get("status").asString()).isEqualTo("CONFIRMED");
        assertThat(accessToken).isNotBlank();

        // Repeated identical request with the same Idempotency-Key must not create a second order.
        String secondResponse = mockMvc.perform(post("/api/v1/orders")
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON).content(requestBody))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        JsonNode second = jsonMapper.readTree(secondResponse);
        assertThat(second.get("order").get("orderId").asString()).isEqualTo(orderId);
        assertThat(second.get("accessToken").isNull()).isTrue();

        boolean appearsInActiveOrders = orderQuery.findActiveOrders().stream()
            .map(OrderView::orderId)
            .anyMatch(id -> id.toString().equals(orderId));
        assertThat(appearsInActiveOrders).isTrue();

        mockMvc.perform(get("/api/v1/orders/" + displayNumber + "/status?token=" + accessToken))
            .andExpect(status().isOk());
    }

    @Test
    void repeatedIdempotencyKeyWithDifferentBodyIsRejected() throws Exception {
        String basketId = readyToCheckoutBasketId();
        String otherBasketId = readyToCheckoutBasketId();
        String idempotencyKey = UUID.randomUUID().toString();

        mockMvc.perform(post("/api/v1/orders")
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"basketId":"%s"}
                    """.formatted(basketId)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/orders")
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"basketId":"%s"}
                    """.formatted(otherBasketId)))
            .andExpect(status().isConflict());
    }

    @Test
    void guestStatusLookupWithoutOrWithWrongTokenIsForbidden() throws Exception {
        String basketId = readyToCheckoutBasketId();
        String requestBody = """
            {"basketId":"%s"}
            """.formatted(basketId);

        String response = mockMvc.perform(post("/api/v1/orders")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON).content(requestBody))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        String displayNumber = jsonMapper.readTree(response).get("order").get("displayNumber").asString();

        mockMvc.perform(get("/api/v1/orders/" + displayNumber + "/status"))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/orders/" + displayNumber + "/status?token=not-the-right-token"))
            .andExpect(status().isForbidden());
    }

    @Test
    void checkoutOfAlreadyCheckedOutBasketIsRejected() throws Exception {
        String basketId = readyToCheckoutBasketId();
        String requestBody = """
            {"basketId":"%s"}
            """.formatted(basketId);

        mockMvc.perform(post("/api/v1/orders")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON).content(requestBody))
            .andExpect(status().isCreated());

        // A different Idempotency-Key against the same, now-checked-out basket must fail.
        mockMvc.perform(post("/api/v1/orders")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON).content(requestBody))
            .andExpect(status().isConflict());
    }
}
