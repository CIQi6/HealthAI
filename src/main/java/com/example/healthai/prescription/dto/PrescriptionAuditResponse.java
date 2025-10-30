package com.example.healthai.prescription.dto;

public record PrescriptionAuditResponse(
    Long id,
    Long prescriptionItemId,
    String checker,
    String result,
    String message,
    String violations,
    String checkTime
) {
}
