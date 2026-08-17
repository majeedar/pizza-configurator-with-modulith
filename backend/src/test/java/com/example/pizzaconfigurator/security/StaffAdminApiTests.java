package com.example.pizzaconfigurator.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.pizzaconfigurator.SharedTestcontainersConfiguration;
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

/** Agent.md §8.3/§9.3 "Staff Users" admin CRUD. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(SharedTestcontainersConfiguration.class)
@Testcontainers
class StaffAdminApiTests {

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
    void adminCreatesListsAndDisablesAStaffAccount() throws Exception {
        String adminToken = adminToken();
        String username = "kitchen-" + UUID.randomUUID();

        String createResponse = mockMvc.perform(post("/api/v1/admin/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"%s","displayName":"New Kitchen Hire","email":null,"password":"correcthorse","role":"KITCHEN"}
                    """.formatted(username)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        JsonNode created = jsonMapper.readTree(createResponse);
        assertThat(created.get("role").asString()).isEqualTo("KITCHEN");
        assertThat(created.get("enabled").asBoolean()).isTrue();
        String employeeId = created.get("employeeId").asString();

        // New account can log in immediately.
        mockMvc.perform(post("/api/v1/staff/login").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"%s","password":"correcthorse"}
                    """.formatted(username)))
            .andExpect(status().isOk());

        String list = mockMvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        boolean found = false;
        for (JsonNode entry : jsonMapper.readTree(list)) {
            if (entry.get("username").asString().equals(username)) {
                found = true;
            }
        }
        assertThat(found).isTrue();

        mockMvc.perform(put("/api/v1/admin/users/" + employeeId + "/enabled")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"enabled":false}
                    """))
            .andExpect(status().isOk());

        // Disabled account can no longer log in.
        mockMvc.perform(post("/api/v1/staff/login").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"%s","password":"correcthorse"}
                    """.formatted(username)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void duplicateUsernameIsRejected() throws Exception {
        String adminToken = adminToken();
        String body = """
            {"username":"kitchen","displayName":"Duplicate","email":null,"password":"correcthorse","role":"KITCHEN"}
            """;
        // "kitchen" is already seeded (V903).
        mockMvc.perform(post("/api/v1/admin/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isConflict());
    }

    @Test
    void nonAdminCannotManageStaff() throws Exception {
        String kitchenToken = mockMvc.perform(post("/api/v1/staff/login").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"kitchen","password":"kitchen123"}
                    """))
            .andReturn().getResponse().getContentAsString();
        String token = jsonMapper.readTree(kitchenToken).get("token").asString();

        mockMvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden());
    }
}
