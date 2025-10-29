package com.example.healthai.consult.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.healthai.audit.AuditConstants;
import com.example.healthai.audit.service.AuditTrailService;
import com.example.healthai.auth.domain.User;
import com.example.healthai.auth.domain.UserType;
import com.example.healthai.auth.mapper.UserMapper;
import com.example.healthai.common.exception.BusinessException;
import com.example.healthai.common.exception.ErrorCode;
import com.example.healthai.consult.domain.Consultation;
import com.example.healthai.consult.domain.ConsultationMessage;
import com.example.healthai.consult.domain.ConsultationMessageRole;
import com.example.healthai.consult.domain.ConsultationStatus;
import com.example.healthai.consult.config.ConsultationKafkaProperties;
import com.example.healthai.consult.dto.ConsultationCreateRequest;
import com.example.healthai.consult.dto.ConsultationDetailResponse;
import com.example.healthai.consult.dto.ConsultationListResponse;
import com.example.healthai.consult.dto.ConsultationMessageResponse;
import com.example.healthai.consult.dto.ConsultationReviewRequest;
import com.example.healthai.consult.mapper.ConsultationMapper;
import com.example.healthai.consult.mapper.ConsultationMessageMapper;
import com.example.healthai.consult.event.ConsultationEventPayload;
import com.example.healthai.prompt.service.PromptExecutionCommand;
import com.example.healthai.prompt.service.PromptResult;
import com.example.healthai.prompt.service.PromptService;

@Service
public class ConsultationService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;
    private static final int MAX_PAGE_SIZE = 100;

    private final ConsultationMapper consultationMapper;
    private final ConsultationMessageMapper consultationMessageMapper;
    private final UserMapper userMapper;
    private final PromptService promptService;
    private final AuditTrailService auditTrailService;
    private final KafkaTemplate<String, ConsultationEventPayload> kafkaTemplate;
    private final ConsultationKafkaProperties kafkaProperties;

    public ConsultationService(ConsultationMapper consultationMapper,
                               ConsultationMessageMapper consultationMessageMapper,
                               UserMapper userMapper,
                               PromptService promptService,
                               AuditTrailService auditTrailService,
                               ObjectProvider<KafkaTemplate<String, ConsultationEventPayload>> kafkaTemplateProvider,
                               ConsultationKafkaProperties kafkaProperties) {
        this.consultationMapper = consultationMapper;
        this.consultationMessageMapper = consultationMessageMapper;
        this.userMapper = userMapper;
        this.promptService = promptService;
        this.auditTrailService = auditTrailService;
        this.kafkaTemplate = kafkaTemplateProvider.getIfAvailable();
        this.kafkaProperties = kafkaProperties;
    }

    @Transactional
    public ConsultationDetailResponse create(String username, ConsultationCreateRequest request) {
        User user = loadUser(username);
        if (user.getUserType() != UserType.PATIENT && user.getUserType() != UserType.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅患者可发起问诊");
        }

        LocalDateTime now = LocalDateTime.now();
        Consultation consultation = Consultation.builder()
            .userId(user.getId())
            .symptomDescription(request.symptomDescription())
            .status(ConsultationStatus.DRAFT)
            .createdAt(now)
            .updatedAt(now)
            .build();
        consultationMapper.insert(consultation);

        saveMessage(consultation.getId(), ConsultationMessageRole.PATIENT, 1, request.symptomDescription(), null);

        auditTrailService.recordConsultationEvent(
            AuditConstants.ACTION_CONSULT_CREATED,
            user.getId(),
            user.getUserType().name(),
            consultation.getId().toString(),
            null,
            null);
        publishEvent(kafkaProperties.getCreatedTopic(), consultation, "CONSULTATION_CREATED", user);

        try {
            PromptResult promptResult = promptService.executeConsultationPrompt(buildPromptCommand(request, user));

            consultation.setAiDiagnosis(promptResult.getContent());
            consultation.setAiModel(promptResult.getModel());
            consultation.setAiLatencyMs(promptResult.getLatencyMs() == null ? null : promptResult.getLatencyMs().intValue());
            consultation.setAiErrorCode(null);
            consultation.setStatus(ConsultationStatus.AI_REVIEWED);
            consultation.setUpdatedAt(LocalDateTime.now());
            consultationMapper.update(consultation);

            saveMessage(consultation.getId(), ConsultationMessageRole.AI, 2, promptResult.getContent(), promptResult.getCompletionTokens());

            auditTrailService.recordConsultationEvent(
                AuditConstants.ACTION_CONSULT_AI_COMPLETED,
                user.getId(),
                user.getUserType().name(),
                consultation.getId().toString(),
                null,
                null);
            publishEvent(kafkaProperties.getAiReviewedTopic(), consultation, "CONSULTATION_AI_REVIEWED", user);
        } catch (BusinessException ex) {
            consultation.setStatus(ConsultationStatus.FAILED);
            consultation.setAiErrorCode(ex.getErrorCode().getCode());
            consultation.setUpdatedAt(LocalDateTime.now());
            consultationMapper.update(consultation);

            auditTrailService.recordConsultationEvent(
                AuditConstants.ACTION_CONSULT_AI_COMPLETED,
                user.getId(),
                user.getUserType().name(),
                consultation.getId().toString(),
                null,
                ex.getMessage());
            publishEvent(kafkaProperties.getFailedTopic(), consultation, "CONSULTATION_FAILED", user);
        }

        return buildDetailResponse(consultation, consultationMessageMapper.findByConsultationId(consultation.getId()));
    }

    @Transactional(readOnly = true)
    public ConsultationListResponse list(String username, String status, int page, int size) {
        User user = loadUser(username);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int offset = safePage * safeSize;

        ConsultationStatus statusFilter = parseStatus(status);

        Long userIdFilter = user.getUserType() == UserType.PATIENT ? user.getId() : null;
        Long doctorIdFilter = user.getUserType() == UserType.DOCTOR ? user.getId() : null;

        List<Consultation> consultations = consultationMapper.search(userIdFilter, doctorIdFilter, statusFilter, safeSize, offset);
        long total = consultationMapper.count(userIdFilter, doctorIdFilter, statusFilter);

        List<ConsultationListResponse.ConsultationSummary> summaries = consultations.stream()
            .map(this::toSummary)
            .toList();

        return new ConsultationListResponse(summaries, total, safePage, safeSize);
    }

    @Transactional(readOnly = true)
    public ConsultationDetailResponse detail(String username, Long id) {
        User user = loadUser(username);
        Consultation consultation = consultationMapper.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.CONSULTATION_NOT_FOUND, "问诊记录不存在"));
        ensureAccess(user, consultation);
        List<ConsultationMessage> messages = consultationMessageMapper.findByConsultationId(id);
        return buildDetailResponse(consultation, messages);
    }

    @Transactional
    public ConsultationDetailResponse review(String username, Long id, ConsultationReviewRequest request) {
        User user = loadUser(username);
        if (user.getUserType() != UserType.DOCTOR && user.getUserType() != UserType.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅医生可复核问诊");
        }

        Consultation consultation = consultationMapper.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.CONSULTATION_NOT_FOUND, "问诊记录不存在"));

        if (consultation.getStatus() == ConsultationStatus.CLOSED || consultation.getStatus() == ConsultationStatus.REJECTED) {
            throw new BusinessException(ErrorCode.CONSULTATION_STATUS_CONFLICT, "问诊已结束，无法复核");
        }

        if (consultation.getStatus() != ConsultationStatus.AI_REVIEWED && consultation.getStatus() != ConsultationStatus.DOCTOR_REVIEWED) {
            throw new BusinessException(ErrorCode.CONSULTATION_STATUS_CONFLICT, "当前问诊状态不允许复核");
        }

        if (consultation.getDoctorId() != null && !consultation.getDoctorId().equals(user.getId()) && user.getUserType() != UserType.ADMIN) {
            throw new BusinessException(ErrorCode.CONSULTATION_FORBIDDEN, "问诊已由其他医生处理");
        }

        ConsultationStatus newStatus = ConsultationStatus.valueOf(request.status());
        LocalDateTime now = LocalDateTime.now();

        consultation.setDoctorId(user.getId());
        consultation.setDoctorOpinion(request.doctorOpinion());
        consultation.setStatus(newStatus);
        consultation.setUpdatedAt(now);
        if (newStatus == ConsultationStatus.CLOSED || newStatus == ConsultationStatus.REJECTED) {
            consultation.setClosedAt(now);
        }

        consultationMapper.update(consultation);

        int nextSequence = calculateNextSequence(id);
        saveMessage(id, ConsultationMessageRole.DOCTOR, nextSequence, request.doctorOpinion(), null);

        auditTrailService.recordConsultationEvent(
            AuditConstants.ACTION_CONSULT_REVIEWED,
            user.getId(),
            user.getUserType().name(),
            id.toString(),
            null,
            newStatus.name());
        publishEvent(kafkaProperties.getReviewedTopic(), consultation, "CONSULTATION_REVIEWED", user);

        return buildDetailResponse(consultation, consultationMessageMapper.findByConsultationId(id));
    }

    @Transactional
    public ConsultationDetailResponse close(String username, Long id) {
        User user = loadUser(username);
        Consultation consultation = consultationMapper.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.CONSULTATION_NOT_FOUND, "问诊记录不存在"));

        ensureAccess(user, consultation);

        if (consultation.getStatus() == ConsultationStatus.CLOSED) {
            return buildDetailResponse(consultation, consultationMessageMapper.findByConsultationId(id));
        }
        if (consultation.getStatus() == ConsultationStatus.REJECTED) {
            throw new BusinessException(ErrorCode.CONSULTATION_STATUS_CONFLICT, "问诊已被拒绝");
        }

        LocalDateTime now = LocalDateTime.now();
        consultation.setStatus(ConsultationStatus.CLOSED);
        consultation.setClosedAt(now);
        consultation.setUpdatedAt(now);
        consultationMapper.update(consultation);

        auditTrailService.recordConsultationEvent(
            AuditConstants.ACTION_CONSULT_CLOSED,
            user.getId(),
            user.getUserType().name(),
            id.toString(),
            null,
            null);
        publishEvent(kafkaProperties.getClosedTopic(), consultation, "CONSULTATION_CLOSED", user);

        return buildDetailResponse(consultation, consultationMessageMapper.findByConsultationId(id));
    }

    private PromptExecutionCommand buildPromptCommand(ConsultationCreateRequest request, User user) {
        Map<String, Object> variables = new HashMap<>(request.variables());
        variables.putIfAbsent("symptomDescription", request.symptomDescription());
        if (request.attachments() != null && !request.attachments().isEmpty()) {
            variables.put("attachments", request.attachments());
        }
        variables.putIfAbsent("patientId", user.getId());
        variables.putIfAbsent("patientName", user.getFullName());

        return PromptExecutionCommand.builder()
            .templateCode(request.templateCode())
            .variables(variables)
            .build();
    }

    private void saveMessage(Long consultationId, ConsultationMessageRole role, int sequence, String content, Integer tokenUsage) {
        LocalDateTime now = LocalDateTime.now();
        ConsultationMessage message = ConsultationMessage.builder()
            .consultationId(consultationId)
            .role(role)
            .sequenceNo(sequence)
            .content(content)
            .tokenUsage(tokenUsage)
            .createdAt(now)
            .updatedAt(now)
            .build();
        consultationMessageMapper.insert(message);
    }

    private ConsultationListResponse.ConsultationSummary toSummary(Consultation consultation) {
        return new ConsultationListResponse.ConsultationSummary(
            consultation.getId(),
            consultation.getStatus().name(),
            consultation.getSymptomDescription(),
            consultation.getAiDiagnosis(),
            consultation.getDoctorOpinion(),
            formatDateTime(consultation.getCreatedAt())
        );
    }

    private ConsultationDetailResponse buildDetailResponse(Consultation consultation, List<ConsultationMessage> messages) {
        return new ConsultationDetailResponse(
            consultation.getId(),
            consultation.getUserId(),
            consultation.getDoctorId(),
            consultation.getSymptomDescription(),
            consultation.getAiDiagnosis(),
            consultation.getDoctorOpinion(),
            consultation.getStatus().name(),
            consultation.getAiModel(),
            consultation.getAiLatencyMs(),
            consultation.getAiErrorCode(),
            formatDateTime(consultation.getCreatedAt()),
            formatDateTime(consultation.getUpdatedAt()),
            formatDateTime(consultation.getClosedAt()),
            messages.stream()
                .map(this::toMessageResponse)
                .toList()
        );
    }

    private ConsultationMessageResponse toMessageResponse(ConsultationMessage message) {
        return new ConsultationMessageResponse(
            message.getId(),
            message.getRole().name(),
            message.getSequenceNo(),
            message.getContent(),
            message.getTokenUsage(),
            formatDateTime(message.getCreatedAt())
        );
    }

    private void publishEvent(String topic, Consultation consultation, String eventType, User actor) {
        if (kafkaTemplate == null) {
            return;
        }
        ConsultationEventPayload payload = new ConsultationEventPayload(
            consultation.getId(),
            consultation.getUserId(),
            consultation.getDoctorId(),
            consultation.getStatus().name(),
            eventType,
            consultation.getSymptomDescription(),
            consultation.getAiDiagnosis(),
            consultation.getDoctorOpinion(),
            consultation.getAiModel(),
            consultation.getAiLatencyMs(),
            consultation.getAiErrorCode(),
            formatDateTime(LocalDateTime.now())
        );
        kafkaTemplate.send(topic, consultation.getId().toString(), payload);
    }

    private int calculateNextSequence(Long consultationId) {
        return consultationMessageMapper.findByConsultationId(consultationId).size() + 1;
    }

    private ConsultationStatus parseStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        try {
            return ConsultationStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "状态参数无效");
        }
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? null : DATE_TIME_FORMATTER.format(dateTime);
    }

    private User loadUser(String username) {
        return userMapper.findByUsername(username)
            .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "未找到用户会话"));
    }

    private void ensureAccess(User user, Consultation consultation) {
        if (user.getUserType() == UserType.ADMIN) {
            return;
        }
        if (user.getUserType() == UserType.PATIENT) {
            if (!consultation.getUserId().equals(user.getId())) {
                throw new BusinessException(ErrorCode.CONSULTATION_FORBIDDEN, "无权访问该问诊记录");
            }
            return;
        }
        if (user.getUserType() == UserType.DOCTOR) {
            if (consultation.getDoctorId() != null && !consultation.getDoctorId().equals(user.getId())) {
                throw new BusinessException(ErrorCode.CONSULTATION_FORBIDDEN, "问诊已分配给其他医生");
            }
        }
    }
}
