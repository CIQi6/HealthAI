package com.example.healthai.audit.dto;

import java.time.LocalDateTime;

public record AuditEventResponse(
        Long id,
        LocalDateTime occurredAt,
        Long actorId,
        String actorType,
        String action,
        String resourceType,
        String resourceId,
        String sourceIp,
        String metadata,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
