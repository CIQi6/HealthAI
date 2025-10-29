package com.example.healthai.consult.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.KafkaTemplate;

import com.example.healthai.audit.service.AuditTrailService;
import com.example.healthai.auth.domain.User;
import com.example.healthai.auth.domain.UserType;
import com.example.healthai.auth.mapper.UserMapper;
import com.example.healthai.common.exception.BusinessException;
import com.example.healthai.common.exception.ErrorCode;
import com.example.healthai.consult.config.ConsultationKafkaProperties;
import com.example.healthai.consult.domain.ConsultationMessage;
import com.example.healthai.consult.domain.ConsultationMessageRole;
import com.example.healthai.consult.domain.ConsultationStatus;
import com.example.healthai.consult.dto.ConsultationCreateRequest;
import com.example.healthai.consult.dto.ConsultationDetailResponse;
import com.example.healthai.consult.event.ConsultationEventPayload;
import com.example.healthai.consult.mapper.ConsultationMapper;
import com.example.healthai.consult.mapper.ConsultationMessageMapper;
import com.example.healthai.prompt.domain.PromptChannel;
import com.example.healthai.prompt.service.PromptExecutionCommand;
import com.example.healthai.prompt.service.PromptResult;
import com.example.healthai.prompt.service.PromptService;

@ExtendWith(MockitoExtension.class)
class ConsultationServiceTest {

    @Mock
    private ConsultationMapper consultationMapper;
    @Mock
    private ConsultationMessageMapper consultationMessageMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PromptService promptService;
    @Mock
    private AuditTrailService auditTrailService;
    @Mock
    private KafkaTemplate<String, ConsultationEventPayload> kafkaTemplate;
    @Mock
    private ObjectProvider<KafkaTemplate<String, ConsultationEventPayload>> kafkaTemplateProvider;

    private ConsultationKafkaProperties kafkaProperties;
    private ConsultationService consultationService;

    @BeforeEach
    void setUp() {
        kafkaProperties = new ConsultationKafkaProperties();
        when(kafkaTemplateProvider.getIfAvailable()).thenReturn(kafkaTemplate);
        consultationService = new ConsultationService(
            consultationMapper,
            consultationMessageMapper,
            userMapper,
            promptService,
            auditTrailService,
            kafkaTemplateProvider,
            kafkaProperties
        );
    }

    @Test
    void shouldCreateConsultationWithAiReview() {
        User user = buildUser(UserType.PATIENT);
        when(userMapper.findByUsername("patient"))
            .thenReturn(Optional.of(user));

        doAnswer(invocation -> {
            var consultation = invocation.getArgument(0, com.example.healthai.consult.domain.Consultation.class);
            consultation.setId(1L);
            return 1;
        }).when(consultationMapper).insert(any());

        when(promptService.executeConsultationPrompt(any(PromptExecutionCommand.class)))
            .thenReturn(PromptResult.builder()
                .prompt("Rendered prompt")
                .channel(PromptChannel.OLLAMA)
                .model("llama3")
                .promptTokens(10)
                .completionTokens(20)
                .latencyMs(120L)
                .content("AI diagnosis suggestion")
                .build());

        when(consultationMessageMapper.findByConsultationId(1L))
            .thenReturn(List.of(
                ConsultationMessage.builder()
                    .id(1L)
                    .consultationId(1L)
                    .role(ConsultationMessageRole.PATIENT)
                    .sequenceNo(1)
                    .content("symptom")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build(),
                ConsultationMessage.builder()
                    .id(2L)
                    .consultationId(1L)
                    .role(ConsultationMessageRole.AI)
                    .sequenceNo(2)
                    .content("AI diagnosis suggestion")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build()
            ));

        ConsultationDetailResponse response = consultationService.create(
            "patient", new ConsultationCreateRequest("symptom", List.of(), null, Map.of()));

        assertThat(response.status()).isEqualTo(ConsultationStatus.AI_REVIEWED.name());
        assertThat(response.aiDiagnosis()).isEqualTo("AI diagnosis suggestion");

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate, times(2)).send(topicCaptor.capture(), anyString(), any(ConsultationEventPayload.class));
        assertThat(topicCaptor.getAllValues())
            .contains(kafkaProperties.getCreatedTopic(), kafkaProperties.getAiReviewedTopic());
    }

    @Test
    void shouldMarkConsultationFailedWhenPromptThrowsException() {
        User user = buildUser(UserType.PATIENT);
        when(userMapper.findByUsername("patient"))
            .thenReturn(Optional.of(user));

        doAnswer(invocation -> {
            var consultation = invocation.getArgument(0, com.example.healthai.consult.domain.Consultation.class);
            consultation.setId(2L);
            return 1;
        }).when(consultationMapper).insert(any());

        when(promptService.executeConsultationPrompt(any(PromptExecutionCommand.class)))
            .thenThrow(new BusinessException(ErrorCode.LLM_TIMEOUT, "timeout"));

        when(consultationMessageMapper.findByConsultationId(2L))
            .thenReturn(List.of(
                ConsultationMessage.builder()
                    .id(10L)
                    .consultationId(2L)
                    .role(ConsultationMessageRole.PATIENT)
                    .sequenceNo(1)
                    .content("symptom")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build()
            ));

        ConsultationDetailResponse response = consultationService.create(
            "patient", new ConsultationCreateRequest("symptom", List.of(), null, Map.of()));

        assertThat(response.status()).isEqualTo(ConsultationStatus.FAILED.name());
        assertThat(response.aiErrorCode()).isEqualTo(ErrorCode.LLM_TIMEOUT.getCode());

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate, times(2)).send(topicCaptor.capture(), anyString(), any(ConsultationEventPayload.class));
        assertThat(topicCaptor.getAllValues())
            .contains(kafkaProperties.getCreatedTopic(), kafkaProperties.getFailedTopic());
    }

    private User buildUser(UserType type) {
        LocalDateTime now = LocalDateTime.now();
        return User.builder()
            .id(99L)
            .username("patient")
            .passwordHash("hash")
            .fullName("Test Patient")
            .gender("unknown")
            .userType(type)
            .registeredAt(now)
            .createdAt(now)
            .updatedAt(now)
            .build();
    }
}
