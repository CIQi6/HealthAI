package com.example.healthai.consult.event;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ConsultationEventPayload(
        Long consultationId,
        Long patientId,
        Long doctorId,
        String status,
        String eventType,
        String summary,
        String aiDiagnosis,
        String doctorOpinion,
        String aiModel,
        Integer aiLatencyMs,
        String aiErrorCode,
        String occurredAt
) {
}
