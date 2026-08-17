package com.example.pizzaconfigurator.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
 * Agent.md §14.4: "audit of admin rule/price/app-link changes" —
 * app-link auditing is covered by {@code AppLinkFlowIntegrationTest}; this
 * covers rule and price changes. Catalog/staff changes are deliberately
 * not audited — §14.4 names only rule/price/app-link, and the broader "at
 * minimum" list in §30 doesn't mention them either.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(SharedTestcontainersConfiguration.class)
@Testcontainers
class AuditTrailIntegrationTest {

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

    @Test
    void ruleCreationIsAudited() throws Exception {
        String adminToken = adminToken();
        String ruleCode = "AUDIT_TEST_" + System.nanoTime();

        // GLOBAL-scope rules apply to every subsequent validation in the
        // shared Testcontainers database used across this whole test run
        // (see SharedTestcontainersConfiguration) — the params must
        // therefore exactly match MaxQuantityEvaluator.Params (a top-level
        // "max", not "maxQuantity") and target an ingredient code no real
        // test pizza ever uses, or every other integration test's
        // validation would break on this rule for the rest of the run.
        mockMvc.perform(post("/api/v1/admin/rules")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"ruleCode":"%s","ruleType":"MAX_QUANTITY","scopeType":"GLOBAL","scopeId":null,
                     "parameters":{"ingredientCode":"AUDIT_TEST_INGREDIENT_XYZ","max":3},
                     "message":"test rule","active":true,"validFrom":null,"validTo":null}
                    """.formatted(ruleCode)))
            .andExpect(status().isCreated());

        String audit = mockMvc.perform(get("/api/v1/admin/audit").header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        boolean found = false;
        for (JsonNode entry : jsonMapper.readTree(audit)) {
            if (entry.get("action").asString().equals("RULE_CREATED")
                && entry.get("afterJson").asString().contains(ruleCode)) {
                found = true;
                assertThat(entry.get("entityType").asString()).isEqualTo("RuleDefinition");
                assertThat(entry.get("actorRole").asString()).isEqualTo("ADMIN");
                assertThat(entry.get("beforeJson").isNull()).isTrue();
            }
        }
        assertThat(found).isTrue();
    }

    @Test
    void priceUpdateIsAudited() throws Exception {
        String adminToken = adminToken();

        String createResponse = mockMvc.perform(post("/api/v1/admin/prices")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"itemType":"INGREDIENT","itemId":"AUDIT_TEST_ING","amount":1.23,"currency":"EUR","active":true,"validFrom":null,"validTo":null}
                    """))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        String priceId = jsonMapper.readTree(createResponse).get("priceId").asString();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/admin/prices/" + priceId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"itemType":"INGREDIENT","itemId":"AUDIT_TEST_ING","amount":9.99,"currency":"EUR","active":true,"validFrom":null,"validTo":null}
                    """))
            .andExpect(status().isOk());

        String audit = mockMvc.perform(get("/api/v1/admin/audit").header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        boolean found = false;
        for (JsonNode entry : jsonMapper.readTree(audit)) {
            if (entry.get("action").asString().equals("PRICE_UPDATED") && entry.get("entityId").asString().equals(priceId)) {
                found = true;
                assertThat(entry.get("beforeJson").asString()).contains("1.23");
                assertThat(entry.get("afterJson").asString()).contains("9.99");
            }
        }
        assertThat(found).isTrue();
    }
}
