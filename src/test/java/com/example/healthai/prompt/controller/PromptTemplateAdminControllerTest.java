package com.example.healthai.prompt.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import com.example.healthai.AbstractIntegrationTest;
import com.example.healthai.auth.domain.UserType;
import com.example.healthai.prompt.domain.PromptChannel;
import com.example.healthai.prompt.domain.PromptTemplate;
import com.example.healthai.prompt.mapper.PromptTemplateMapper;
import com.fasterxml.jackson.databind.JsonNode;

class PromptTemplateAdminControllerTest extends AbstractIntegrationTest {

    @Autowired
    private PromptTemplateMapper promptTemplateMapper;

    private String adminToken;

    @BeforeEach
    void setupAdmin() throws Exception {
        createUser("admin-user", "Password123", UserType.ADMIN);
        adminToken = loginAndGetToken("admin-user", "Password123");
    }

    @Test
    void shouldListOnlyActiveTemplatesByDefault() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        PromptTemplate enabledTemplate = PromptTemplate.builder()
            .code("consult.initial")
            .channel(PromptChannel.OLLAMA)
            .modelName("llama3")
            .language("zh-CN")
            .version("v1")
            .description("active template")
            .content("Hello {{name}}")
            .variables("{\"name\":\"Patient\"}")
            .enabled(true)
            .createdAt(now)
            .updatedAt(now)
            .build();
        promptTemplateMapper.insert(enabledTemplate);

        PromptTemplate disabledTemplate = PromptTemplate.builder()
            .code("consult.initial")
            .channel(PromptChannel.OLLAMA)
            .modelName("llama3")
            .language("zh-CN")
            .version("v0")
            .description("disabled template")
            .content("Hi")
            .variables("{}")
            .enabled(false)
            .createdAt(now)
            .updatedAt(now)
            .build();
        promptTemplateMapper.insert(disabledTemplate);

        JsonNode response = objectMapper.readTree(mockMvc.perform(get("/api/v1/admin/prompt-templates")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString());

        JsonNode activeTemplates = response.path("data");
        assertThat(activeTemplates).hasSize(1);
        assertThat(activeTemplates.get(0).path("description").asText()).isEqualTo("active template");

        JsonNode responseAll = objectMapper.readTree(mockMvc.perform(get("/api/v1/admin/prompt-templates")
                .header("Authorization", "Bearer " + adminToken)
                .param("includeDisabled", "true"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString());

        assertThat(responseAll.path("data")).hasSize(2);
    }

    @Test
    void shouldCreateUpdateAndDisableTemplate() throws Exception {
        String requestBody = objectMapper.createObjectNode()
            .put("code", "consult.followup")
            .put("version", "v1")
            .put("channel", PromptChannel.OLLAMA.name())
            .put("modelName", "llama3")
            .put("language", "zh-CN")
            .put("description", "follow up")
            .put("content", "Prompt with {{variable}}").toString();

        JsonNode createResponse = objectMapper.readTree(mockMvc.perform(post("/api/v1/admin/prompt-templates")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString()).path("data");

        Long templateId = createResponse.path("id").asLong();
        assertThat(templateId).isPositive();

        String updateBody = objectMapper.createObjectNode()
            .put("code", "consult.followup")
            .put("version", "v2")
            .put("channel", PromptChannel.OLLAMA.name())
            .put("modelName", "llama3")
            .put("language", "zh-CN")
            .put("description", "follow up updated")
            .put("content", "Updated content")
            .put("enabled", true)
            .toString();

        JsonNode updateResponse = objectMapper.readTree(mockMvc.perform(put("/api/v1/admin/prompt-templates/" + templateId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString()).path("data");

        assertThat(updateResponse.path("version").asText()).isEqualTo("v2");
        assertThat(updateResponse.path("description").asText()).isEqualTo("follow up updated");

        JsonNode statusResponse = objectMapper.readTree(mockMvc.perform(patch("/api/v1/admin/prompt-templates/" + templateId + "/status")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.createObjectNode().put("enabled", false).toString()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString()).path("data");

        assertThat(statusResponse.path("enabled").asBoolean()).isFalse();
    }
}
