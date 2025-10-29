package com.example.healthai.consult.dto;

import java.util.List;

public record ConsultationListResponse(
        List<ConsultationSummary> items,
        long total,
        int page,
        int size
) {

    public record ConsultationSummary(
            Long id,
            String status,
            String symptomDescription,
            String aiDiagnosis,
            String doctorOpinion,
            String createdAt
    ) {
    }
}
