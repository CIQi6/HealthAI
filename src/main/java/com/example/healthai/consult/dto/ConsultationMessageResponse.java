package com.example.healthai.consult.dto;

public record ConsultationMessageResponse(
        Long id,
        String role,
        Integer sequenceNo,
        String content,
        Integer tokenUsage,
        String createdAt
) {
}
