package com.example.healthai.audit.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.healthai.audit.domain.AuditEvent;
import com.example.healthai.audit.dto.AuditEventResponse;
import com.example.healthai.audit.mapper.AuditEventMapper;
import com.example.healthai.common.api.PageResponse;

@Service
public class AuditEventAdminService {

    private static final int MAX_PAGE_SIZE = 200;

    private final AuditEventMapper auditEventMapper;

    public AuditEventAdminService(AuditEventMapper auditEventMapper) {
        this.auditEventMapper = auditEventMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditEventResponse> search(String resourceType,
                                                   String action,
                                                   Long actorId,
                                                   LocalDateTime from,
                                                   LocalDateTime to,
                                                   int page,
                                                   int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int offset = safePage * safeSize;

        List<AuditEvent> events = auditEventMapper.search(resourceType, action, actorId, from, to, safeSize, offset);
        long total = auditEventMapper.count(resourceType, action, actorId, from, to);

        List<AuditEventResponse> responses = events.stream()
            .map(this::toResponse)
            .toList();
        return new PageResponse<>(responses, total, safePage, safeSize);
    }

    private AuditEventResponse toResponse(AuditEvent event) {
        return new AuditEventResponse(
            event.getId(),
            event.getOccurredAt(),
            event.getActorId(),
            event.getActorType(),
            event.getAction(),
            event.getResourceType(),
            event.getResourceId(),
            event.getSourceIp(),
            event.getMetadata(),
            event.getCreatedAt(),
            event.getUpdatedAt()
        );
    }
}
