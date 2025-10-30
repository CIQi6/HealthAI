package com.example.healthai.prescription.dto;

import java.math.BigDecimal;

public record PrescriptionItemResponse(
    Long id,
    Long medicineId,
    String genericName,
    String brandName,
    String dosageInstruction,
    String frequency,
    Integer daySupply,
    BigDecimal quantity,
    String contraResult,
    String contraMessage
) {
}
