package com.example.healthai.auth.controller;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.fasterxml.jackson.databind.JsonNode;

import com.example.healthai.auth.domain.User;
import com.example.healthai.auth.domain.UserType;
import com.example.healthai.auth.mapper.UserMapper;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;

import com.example.healthai.AbstractIntegrationTest;


public class AuthControllerTest extends AbstractIntegrationTest {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    protected void registerUser(String username) throws Exception {
        String payload = "{" +
                "\"username\":\"" + username + "\"," +
                "\"password\":\"Password123\"," +
                "\"fullName\":\"Test User\"," +
                "\"gender\":\"unknown\"," +
                "\"phone\":\"13800000000\"," +
                "\"email\":\"" + username + "@example.com\"" +
                "}";

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRegisterUser() throws Exception {
        String payload = "{" +
                "\"username\":\"testuser\"," +
                "\"password\":\"Password123\"," +
                "\"fullName\":\"Test User\"," +
                "\"gender\":\"unknown\"," +
                "\"phone\":\"13800138000\"," +
                "\"email\":\"test@example.com\"" +
                "}";

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.username").value("testuser"));
    }

    @Test
    void shouldLoginWithValidCredentials() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        User user = User.builder()
                .username("loginuser")
                .passwordHash(passwordEncoder.encode("Password123"))
                .fullName("Login User")
                .gender("unknown")
                .userType(UserType.PATIENT)
                .registeredAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();
        userMapper.insert(user);

        String payload = "{" +
                "\"username\":\"loginuser\"," +
                "\"password\":\"Password123\"" +
                "}";

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(jsonPath("$.data.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.data.accessTokenExpiresInSeconds", notNullValue()))
                .andExpect(jsonPath("$.data.refreshTokenExpiresInSeconds", notNullValue()));
    }

    @Test
    void shouldGetCurrentProfile() throws Exception {
        String registerPayload = "{" +
                "\"username\":\"profileuser\"," +
                "\"password\":\"Password123\"," +
                "\"fullName\":\"Profile User\"," +
                "\"gender\":\"unknown\"," +
                "\"phone\":\"13900139000\"," +
                "\"email\":\"profile@example.com\"" +
                "}";

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload))
                .andExpect(status().isOk());

        String token = loginAndGetToken("profileuser", "Password123");

        mockMvc.perform(get("/api/v1/auth/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("profileuser"));
    }

    @Test
    void shouldRefreshToken() throws Exception {
        registerUser("refreshuser");

        JsonNode authData = loginAndGetAuthData("refreshuser", "Password123");
        String refreshToken = authData.path("refreshToken").asText();

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"refreshToken\":\"" + refreshToken + "\"" +
                                "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(jsonPath("$.data.refreshToken", notNullValue()));
    }

    @Test
    void shouldRevokeRefreshTokenOnLogout() throws Exception {
        registerUser("logoutuser");

        JsonNode authData = loginAndGetAuthData("logoutuser", "Password123");
        String refreshToken = authData.path("refreshToken").asText();

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"refreshToken\":\"" + refreshToken + "\"" +
                                "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"refreshToken\":\"" + refreshToken + "\"" +
                                "}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }
}
