package com.example.healthai.prescription.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PrescriptionItemRequest(
    @NotNull(message = "药品ID不能为空")
    Long drugId,
    @NotNull(message = "用药说明不能为空")
    String dosageInstruction,
    String frequency,
    @NotNull(message = "用药天数不能为空")
    @Min(value = 1, message = "用药天数必须大于0")
    Integer daySupply,
    BigDecimal quantity
) {
}
