package com.example.pizzaconfigurator.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
import tools.jackson.databind.json.JsonMapper;

/**
 * Demo staff accounts ("kitchen"/"kitchen123", "admin"/"admin123") come
 * from the local-profile-only dev seed migration
 * ({@code V903__seed_demo_staff.sql}).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(SharedTestcontainersConfiguration.class)
@Testcontainers
class StaffAuthApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void kitchenStaffCanLoginAndReachTheProductionBoard() throws Exception {
        String token = login("kitchen", "kitchen123");

        mockMvc.perform(get("/api/v1/kitchen/orders").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
    }

    @Test
    void adminCanAlsoReachTheKdsViews() throws Exception {
        String token = login("admin", "admin123");

        mockMvc.perform(get("/api/v1/kitchen/orders").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/staff/login").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"kitchen","password":"wrongpassword"}
                    """))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void unknownUsernameIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/staff/login").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"nobody","password":"whatever"}
                    """))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void kitchenRequestsWithoutAnyTokenAreRejected() throws Exception {
        mockMvc.perform(get("/api/v1/kitchen/orders")).andExpect(status().isUnauthorized());
    }

    @Test
    void kitchenStaffCannotReachAdminOnlyEndpoints() throws Exception {
        String token = login("kitchen", "kitchen123");
        mockMvc.perform(get("/api/v1/admin/does-not-exist-but-role-check-runs-first").header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden());
    }

    private String login(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/api/v1/staff/login").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"%s","password":"%s"}
                    """.formatted(username, password)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andReturn().getResponse().getContentAsString();
        return jsonMapper.readTree(response).get("token").asString();
    }
}
