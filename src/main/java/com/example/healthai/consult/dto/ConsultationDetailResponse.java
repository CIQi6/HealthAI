package com.example.healthai.consult.dto;

import java.util.List;

public record ConsultationDetailResponse(
        Long id,
        Long userId,
        Long doctorId,
        String symptomDescription,
        String aiDiagnosis,
        String doctorOpinion,
        String status,
        String aiModel,
        Integer aiLatencyMs,
        String aiErrorCode,
        String createdAt,
        String updatedAt,
        String closedAt,
        List<ConsultationMessageResponse> messages
) {
}
