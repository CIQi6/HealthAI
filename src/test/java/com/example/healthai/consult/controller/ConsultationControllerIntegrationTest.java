package com.example.healthai.consult.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import com.example.healthai.AbstractIntegrationTest;
import com.example.healthai.auth.domain.User;
import com.example.healthai.auth.domain.UserType;
import com.example.healthai.consult.config.ConsultationKafkaProperties;
import com.example.healthai.consult.domain.Consultation;
import com.example.healthai.consult.domain.ConsultationMessage;
import com.example.healthai.consult.domain.ConsultationMessageRole;
import com.example.healthai.consult.domain.ConsultationStatus;
import com.example.healthai.consult.event.ConsultationEventPayload;
import com.example.healthai.consult.mapper.ConsultationMapper;
import com.example.healthai.consult.mapper.ConsultationMessageMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.concurrent.CompletableFuture;
import org.mockito.ArgumentCaptor;

class ConsultationControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ConsultationMapper consultationMapper;

    @Autowired
    private ConsultationMessageMapper consultationMessageMapper;

    @Autowired
    private ConsultationKafkaProperties kafkaProperties;

    @MockBean
    private KafkaTemplate<String, ConsultationEventPayload> kafkaTemplate;

    @BeforeEach
    void setupKafkaTemplate() {
        reset(kafkaTemplate);
        CompletableFuture<SendResult<String, ConsultationEventPayload>> future = CompletableFuture.completedFuture(null);
        doReturn(future).when(kafkaTemplate).send(anyString(), anyString(), any(ConsultationEventPayload.class));
    }

    @Test
    void shouldCreateConsultationAndTriggerAiReview() throws Exception {
        User user = createUser("patient-user");
        String token = loginAndGetToken("patient-user", "Password123");

        JsonNode response = objectMapper.readTree(mockMvc.perform(post("/api/v1/consultations")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" +
                    "\"symptomDescription\":\"持续咳嗽\"" +
                    "}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString());

        assertThat(response.path("data").path("status").asText()).isEqualTo(ConsultationStatus.AI_REVIEWED.name());
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate, times(2)).send(topicCaptor.capture(), anyString(), any(ConsultationEventPayload.class));
        assertThat(topicCaptor.getAllValues()).containsExactly(
            kafkaProperties.getCreatedTopic(),
            kafkaProperties.getAiReviewedTopic()
        );
    }

    @Test
    void shouldAllowDoctorReviewFlow() throws Exception {
        User patient = createUser("patient-2");
        User doctor = User.builder()
            .username("doctor-1")
            .passwordHash(passwordEncoder.encode("Password123"))
            .fullName("Doctor One")
            .gender("unknown")
            .userType(UserType.DOCTOR)
            .registeredAt(LocalDateTime.now())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        doctor.setId(200L);
        userMapper.insert(doctor);

        Consultation consultation = Consultation.builder()
            .id(10L)
            .userId(patient.getId())
            .symptomDescription("头痛")
            .status(ConsultationStatus.AI_REVIEWED)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        consultationMapper.insert(consultation);

        consultationMessageMapper.insert(ConsultationMessage.builder()
            .consultationId(consultation.getId())
            .role(ConsultationMessageRole.AI)
            .sequenceNo(1)
            .content("AI 初诊")
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build());

        String doctorToken = loginAndGetToken("doctor-1", "Password123");

        JsonNode reviewResponse = objectMapper.readTree(mockMvc.perform(post("/api/v1/consultations/" + consultation.getId() + "/review")
                .header("Authorization", "Bearer " + doctorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" +
                    "\"doctorOpinion\":\"建议休息\"," +
                    "\"status\":\"DOCTOR_REVIEWED\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString());

        assertThat(reviewResponse.path("data").path("status").asText()).isEqualTo(ConsultationStatus.DOCTOR_REVIEWED.name());
        verify(kafkaTemplate, times(1)).send(eq(kafkaProperties.getReviewedTopic()), eq(consultation.getId().toString()),
            any(ConsultationEventPayload.class));
    }

    @Test
    void shouldRejectUnauthorizedAccess() throws Exception {
        mockMvc.perform(get("/api/v1/consultations"))
            .andExpect(status().isUnauthorized());
    }
}
