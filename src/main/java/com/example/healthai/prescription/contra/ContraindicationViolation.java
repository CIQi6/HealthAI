package com.example.healthai.prescription.contra;

import com.example.healthai.prescription.domain.PrescriptionContraStatus;

public record ContraindicationViolation(
    int itemIndex,
    Long medicineId,
    String medicineName,
    String type,
    PrescriptionContraStatus level,
    String message
) {
}
