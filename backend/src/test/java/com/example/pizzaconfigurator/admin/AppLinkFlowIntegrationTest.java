package com.example.pizzaconfigurator.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.pizzaconfigurator.SharedTestcontainersConfiguration;
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
 * Agent.md Phase 11 Definition of Done: admin can change app-link URLs and
 * changes are reflected safely — proven here via the full round-trip
 * (admin PUT → public GET/QR → kitchen GET → audit trail), not just that
 * the write succeeded.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(SharedTestcontainersConfiguration.class)
@Testcontainers
class AppLinkFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    private String adminToken() throws Exception {
        String response = mockMvc.perform(post("/api/v1/staff/login").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"admin","password":"admin123"}
                    """))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return jsonMapper.readTree(response).get("token").asString();
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
    void adminChangesTheCustomerAppLinkAndItIsReflectedEverywhere() throws Exception {
        String adminToken = adminToken();
        String newUrl = "https://example.test/downloads/customer-v2.apk";

        String updateResponse = mockMvc.perform(put("/api/v1/admin/app-links/android/customer")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"url":"%s","active":true}
                    """.formatted(newUrl)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        JsonNode updated = jsonMapper.readTree(updateResponse);
        assertThat(updated.get("url").asString()).isEqualTo(newUrl);
        assertThat(updated.get("audience").asString()).isEqualTo("CUSTOMER");

        // Public, unauthenticated — the customer web app's "Get the app" banner.
        String publicView = mockMvc.perform(get("/api/v1/app-links/android/customer"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        assertThat(jsonMapper.readTree(publicView).get("url").asString()).isEqualTo(newUrl);

        byte[] qrPng = mockMvc.perform(get("/api/v1/app-links/android/customer/qr.png"))
            .andExpect(status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().contentType("image/png"))
            .andReturn().getResponse().getContentAsByteArray();
        assertThat(qrPng.length).isGreaterThan(0);
        // PNG magic bytes.
        assertThat(qrPng[0]).isEqualTo((byte) 0x89);
        assertThat(qrPng[1]).isEqualTo((byte) 'P');
        assertThat(qrPng[2]).isEqualTo((byte) 'N');
        assertThat(qrPng[3]).isEqualTo((byte) 'G');

        // Kitchen KDS corner display (agent.md §8.2).
        String kitchenView = mockMvc.perform(get("/api/v1/kitchen/app-links/android/customer")
                .header("Authorization", "Bearer " + kitchenToken()))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        assertThat(jsonMapper.readTree(kitchenView).get("url").asString()).isEqualTo(newUrl);

        // Admin list reflects the change too.
        String list = mockMvc.perform(get("/api/v1/admin/app-links").header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        JsonNode listNode = jsonMapper.readTree(list);
        boolean foundUpdated = false;
        for (JsonNode entry : listNode) {
            if (entry.get("audience").asString().equals("CUSTOMER")) {
                foundUpdated = entry.get("url").asString().equals(newUrl);
            }
        }
        assertThat(foundUpdated).isTrue();

        // Audit trail (agent.md §14.4 "audit of admin ... app-link changes").
        String audit = mockMvc.perform(get("/api/v1/admin/audit").header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        boolean hasAppLinkAudit = false;
        for (JsonNode entry : jsonMapper.readTree(audit)) {
            if (entry.get("action").asString().equals("APP_LINK_UPDATED") && entry.get("entityType").asString().equals("AppLinkSetting")) {
                hasAppLinkAudit = true;
                assertThat(entry.get("actorId").asString()).isEqualTo("admin");
                assertThat(entry.get("afterJson").asString()).contains(newUrl);
            }
        }
        assertThat(hasAppLinkAudit).isTrue();
    }

    @Test
    void kitchenStaffCannotChangeAppLinks() throws Exception {
        mockMvc.perform(put("/api/v1/admin/app-links/android/customer")
                .header("Authorization", "Bearer " + kitchenToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"url":"https://example.test/x.apk","active":true}
                    """))
            .andExpect(status().isForbidden());
    }

    @Test
    void anonymousCannotChangeAppLinks() throws Exception {
        mockMvc.perform(put("/api/v1/admin/app-links/android/customer")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"url":"https://example.test/x.apk","active":true}
                    """))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void unknownAudienceIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/app-links/android/tablet")).andExpect(status().isBadRequest());
    }

    @Test
    void inactiveAppLinkIsNotServedPublicly() throws Exception {
        String adminToken = adminToken();
        mockMvc.perform(put("/api/v1/admin/app-links/android/kitchen")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"url":"https://example.test/kitchen.apk","active":false}
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/app-links/android/kitchen")).andExpect(status().isNotFound());

        // Restore for other tests sharing the seeded row.
        mockMvc.perform(put("/api/v1/admin/app-links/android/kitchen")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"url":"https://example.test/kitchen.apk","active":true}
                    """))
            .andExpect(status().isOk());
    }
}
