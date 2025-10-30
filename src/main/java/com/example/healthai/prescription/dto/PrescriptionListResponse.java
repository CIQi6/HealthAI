package com.example.healthai.prescription.dto;

import java.util.List;

public record PrescriptionListResponse(
    List<PrescriptionSummary> items,
    long total,
    int page,
    int size
) {

    public record PrescriptionSummary(
        Long id,
        Long consultationId,
        Long patientId,
        Long doctorId,
        String status,
        String contraStatus,
        String createdAt,
        String updatedAt
    ) {
    }
}
