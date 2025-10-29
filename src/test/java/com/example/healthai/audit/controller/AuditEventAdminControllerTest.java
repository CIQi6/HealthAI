package com.example.healthai.audit.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.example.healthai.AbstractIntegrationTest;
import com.example.healthai.audit.AuditConstants;
import com.example.healthai.audit.domain.AuditEvent;
import com.example.healthai.audit.mapper.AuditEventMapper;
import com.example.healthai.auth.domain.UserType;
import com.fasterxml.jackson.databind.JsonNode;

class AuditEventAdminControllerTest extends AbstractIntegrationTest {

    @Autowired
    private AuditEventMapper auditEventMapper;

    private String adminToken;

    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

    @BeforeEach
    void setupAdmin() throws Exception {
        createUser("audit-admin", "Password123", UserType.ADMIN);
        adminToken = loginAndGetToken("audit-admin", "Password123");
    }

    @Test
    void shouldQueryAuditEventsWithFilters() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        insertEvent(now.minusMinutes(3), 101L, AuditConstants.RESOURCE_CONSULTATION, AuditConstants.ACTION_CONSULT_CREATED);
        insertEvent(now.minusMinutes(2), 102L, AuditConstants.RESOURCE_CONSULTATION, AuditConstants.ACTION_CONSULT_AI_COMPLETED);
        insertEvent(now.minusMinutes(1), 103L, AuditConstants.RESOURCE_AUTH, AuditConstants.ACTION_AUTH_LOGIN_SUCCESS);

        JsonNode response = objectMapper.readTree(mockMvc.perform(get("/api/v1/admin/audit-events")
                .header("Authorization", "Bearer " + adminToken)
                .param("resourceType", AuditConstants.RESOURCE_CONSULTATION)
                .param("from", formatter.format(now.minusMinutes(5)))
                .param("to", formatter.format(now))
                .param("size", "5"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString())
            .path("data");

        assertThat(response.path("totalElements").asLong()).isEqualTo(2L);
        JsonNode content = response.path("content");
        assertThat(content).hasSize(2);
        assertThat(content.get(0).path("resourceType").asText()).isEqualTo(AuditConstants.RESOURCE_CONSULTATION);

        JsonNode actorFiltered = objectMapper.readTree(mockMvc.perform(get("/api/v1/admin/audit-events")
                .header("Authorization", "Bearer " + adminToken)
                .param("actorId", "101")
                .param("size", "5"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString())
            .path("data");

        assertThat(actorFiltered.path("totalElements").asLong()).isEqualTo(1L);
        assertThat(actorFiltered.path("content").get(0).path("actorId").asLong()).isEqualTo(101L);
    }

    private void insertEvent(LocalDateTime occurredAt,
                             Long actorId,
                             String resourceType,
                             String action) {
        AuditEvent event = AuditEvent.builder()
            .occurredAt(occurredAt)
            .actorId(actorId)
            .actorType("ADMIN")
            .action(action)
            .resourceType(resourceType)
            .resourceId("resource-" + actorId)
            .sourceIp("127.0.0.1")
            .metadata("{}")
            .createdAt(occurredAt)
            .updatedAt(occurredAt)
            .build();
        auditEventMapper.insert(event);
    }
}
