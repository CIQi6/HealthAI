package com.example.healthai.profile.controller;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.healthai.AbstractIntegrationTest;
import com.example.healthai.auth.controller.AuthControllerTest;

class HealthProfileControllerTest extends AuthControllerTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void shouldCreateOrUpdateProfile() throws Exception {
        String username = "profilecreate";
        registerUser(username);

        String payload = "{" +
                "\"birthDate\":\"1990-01-01\"," +
                "\"bloodType\":\"A\"," +
                "\"chronicDiseases\":\"Hypertension\"," +
                "\"allergyHistory\":\"Peanuts\"," +
                "\"geneticRisk\":\"None\"" +
                "}";

        String token = loginAndGetToken(username, "Password123");

        mockMvc.perform(post("/api/v1/health-profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.bloodType", is("A")));
    }

    @Test
    void shouldGetCurrentProfile() throws Exception {
        String username = "profileview";
        registerUser(username);

        String token = loginAndGetToken(username, "Password123");

        mockMvc.perform(post("/api/v1/health-profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"birthDate\":\"1992-05-20\"," +
                                "\"bloodType\":\"B\"," +
                                "\"chronicDiseases\":\"Asthma\"," +
                                "\"allergyHistory\":\"None\"," +
                                "\"geneticRisk\":\"Low\"" +
                                "}")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/health-profiles")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bloodType", is("B")));
    }

    @Test
    void shouldDeleteProfile() throws Exception {
        String username = "profiledelete";
        registerUser(username);
        String token = loginAndGetToken(username, "Password123");

        String response = mockMvc.perform(post("/api/v1/health-profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"bloodType\":\"O\"" +
                                "}")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long profileId = objectMapper.readTree(response).path("data").path("id").asLong();

        mockMvc.perform(delete("/api/v1/health-profiles/" + profileId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }
}
