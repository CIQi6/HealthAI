package com.example.healthai.prescription.dto;

import java.util.List;

public record PrescriptionDetailResponse(
    Long id,
    Long consultationId,
    Long patientId,
    Long doctorId,
    String status,
    String contraStatus,
    String contraFailReason,
    String notes,
    String createdAt,
    String updatedAt,
    List<PrescriptionItemResponse> items,
    List<PrescriptionAuditResponse> audits
) {
}
