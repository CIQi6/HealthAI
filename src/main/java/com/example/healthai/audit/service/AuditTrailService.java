package com.example.healthai.audit.service;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.healthai.audit.AuditConstants;
import com.example.healthai.audit.domain.AuditEvent;
import com.example.healthai.audit.mapper.AuditEventMapper;

@Service
public class AuditTrailService {

    private static final Logger log = LoggerFactory.getLogger(AuditTrailService.class);

    private final AuditEventMapper auditEventMapper;

    public AuditTrailService(AuditEventMapper auditEventMapper) {
        this.auditEventMapper = auditEventMapper;
    }

    public void recordAuthEvent(String action, Long actorId, String actorType, String resourceId, String metadata) {
        record(action, AuditConstants.RESOURCE_AUTH, actorId, actorType, resourceId, null, metadata);
    }

    public void recordHealthProfileEvent(String action, Long actorId, String actorType, String resourceId, String metadata) {
        record(action, AuditConstants.RESOURCE_HEALTH_PROFILE, actorId, actorType, resourceId, null, metadata);
    }

    public void recordConsultationEvent(String action,
                                        Long actorId,
                                        String actorType,
                                        String resourceId,
                                        String sourceIp,
                                        String metadata) {
        record(action, AuditConstants.RESOURCE_CONSULTATION, actorId, actorType, resourceId, sourceIp, metadata);
    }

    public void record(String action,
                       String resourceType,
                       Long actorId,
                       String actorType,
                       String resourceId,
                       String sourceIp,
                       String metadata) {
        try {
            LocalDateTime now = LocalDateTime.now();
            AuditEvent event = AuditEvent.builder()
                .occurredAt(now)
                .actorId(actorId)
                .actorType(actorType)
                .action(action)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .sourceIp(sourceIp)
                .metadata(metadata)
                .createdAt(now)
                .updatedAt(now)
                .build();
            auditEventMapper.insert(event);
        } catch (Exception ex) {
            log.warn("Failed to record audit event action={} resourceType={} metadata={}", action, resourceType, metadata, ex);
        }
    }
}
