package com.example.pizzaconfigurator.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.pizzaconfigurator.SharedTestcontainersConfiguration;
import com.example.pizzaconfigurator.catalog.api.CatalogQuery;
import com.example.pizzaconfigurator.catalog.api.PizzaSummary;
import com.example.pizzaconfigurator.notification.domain.NotificationChannel;
import com.example.pizzaconfigurator.notification.domain.NotificationRecord;
import com.example.pizzaconfigurator.notification.domain.NotificationStatus;
import com.example.pizzaconfigurator.notification.domain.NotificationType;
import com.example.pizzaconfigurator.notification.infrastructure.persistence.NotificationRecordRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

/**
 * Agent.md Phase 8 Definition of Done: the READY event generates
 * independent Gmail SMTP and FCM push notification attempts without
 * blocking the order transaction. No real Gmail/Firebase credentials exist
 * in this test environment, so both providers stub the actual send
 * (agent.md §27 local default) — this test proves the real end-to-end
 * wiring (events → fan-out → persisted NotificationRecord rows), while
 * {@code NotificationServiceTest} proves channel-failure isolation with
 * fake providers.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(SharedTestcontainersConfiguration.class)
@Testcontainers
class NotificationFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private CatalogQuery catalogQuery;

    @Autowired
    private NotificationRecordRepository notificationRecords;

    private UUID margheritaId() {
        return catalogQuery.findActivePizzas().stream()
            .filter(p -> p.code().equals("MARGHERITA"))
            .map(PizzaSummary::pizzaId)
            .findFirst().orElseThrow();
    }

    @Test
    void readyEventProducesIndependentEmailAndPushNotificationRecords() throws Exception {
        String email = "notify-" + UUID.randomUUID() + "@example.com";
        String registerBody = """
            {"name":"Notify Test","email":"%s","phoneNumber":null,"password":"correcthorse"}
            """.formatted(email);
        String registerResponse = mockMvc.perform(post("/api/v1/customers/register")
                .contentType(MediaType.APPLICATION_JSON).content(registerBody))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        String customerToken = jsonMapper.readTree(registerResponse).get("token").asString();

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
                .header("Authorization", "Bearer " + customerToken)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"basketId":"%s","fcmDeviceToken":"test-device-token-abc"}
                    """.formatted(basketId)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        String orderId = jsonMapper.readTree(orderResponse).get("order").get("orderId").asString();

        String kitchenLoginResponse = mockMvc.perform(post("/api/v1/staff/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"kitchen","password":"kitchen123"}
                    """))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        String kitchenToken = jsonMapper.readTree(kitchenLoginResponse).get("token").asString();

        mockMvc.perform(post("/api/v1/kitchen/orders/" + orderId + "/approve").header("Authorization", "Bearer " + kitchenToken))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/kitchen/orders/" + orderId + "/start").header("Authorization", "Bearer " + kitchenToken))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/kitchen/orders/" + orderId + "/ready").header("Authorization", "Bearer " + kitchenToken))
            .andExpect(status().isOk());

        // @TransactionalEventListener(AFTER_COMMIT) runs synchronously,
        // still within the kitchen HTTP request's thread, immediately once
        // that request's own transaction commits — strictly before MockMvc
        // returns the response above, so no polling/waiting is needed here.
        List<NotificationRecord> readyNotifications = notificationRecords.findByOrderId(UUID.fromString(orderId)).stream()
            .filter(r -> r.getType() == NotificationType.ORDER_READY)
            .toList();
        assertThat(readyNotifications).hasSize(2);

        NotificationRecord emailRecord = readyNotifications.stream()
            .filter(r -> r.getChannel() == NotificationChannel.EMAIL).findFirst().orElseThrow();
        NotificationRecord pushRecord = readyNotifications.stream()
            .filter(r -> r.getChannel() == NotificationChannel.PUSH).findFirst().orElseThrow();

        assertThat(emailRecord.getRecipient()).isEqualTo(email);
        assertThat(emailRecord.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(pushRecord.getRecipient()).isEqualTo("test-device-token-abc");
        assertThat(pushRecord.getStatus()).isEqualTo(NotificationStatus.SENT);
    }
}
