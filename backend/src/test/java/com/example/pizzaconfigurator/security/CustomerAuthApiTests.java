package com.example.pizzaconfigurator.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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

@SpringBootTest
@AutoConfigureMockMvc
@Import(SharedTestcontainersConfiguration.class)
@Testcontainers
class CustomerAuthApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void registerThenLoginIssuesTokens() throws Exception {
        String email = "alice-" + UUID.randomUUID() + "@example.com";
        String registerBody = """
            {"name":"Alice","email":"%s","phoneNumber":"+491234","password":"correcthorse"}
            """.formatted(email);

        mockMvc.perform(post("/api/v1/customers/register").contentType(MediaType.APPLICATION_JSON).content(registerBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.email").value(email));

        String loginBody = """
            {"email":"%s","password":"correcthorse"}
            """.formatted(email);

        mockMvc.perform(post("/api/v1/customers/login").contentType(MediaType.APPLICATION_JSON).content(loginBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void duplicateEmailIsRejected() throws Exception {
        String email = "bob-" + UUID.randomUUID() + "@example.com";
        String body = """
            {"name":"Bob","email":"%s","phoneNumber":null,"password":"correcthorse"}
            """.formatted(email);

        mockMvc.perform(post("/api/v1/customers/register").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/customers/register").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isConflict());
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        String email = "carol-" + UUID.randomUUID() + "@example.com";
        String registerBody = """
            {"name":"Carol","email":"%s","phoneNumber":null,"password":"correcthorse"}
            """.formatted(email);
        mockMvc.perform(post("/api/v1/customers/register").contentType(MediaType.APPLICATION_JSON).content(registerBody))
            .andExpect(status().isCreated());

        String wrongLoginBody = """
            {"email":"%s","password":"wrongpassword"}
            """.formatted(email);
        mockMvc.perform(post("/api/v1/customers/login").contentType(MediaType.APPLICATION_JSON).content(wrongLoginBody))
            .andExpect(status().isUnauthorized());
    }
}
