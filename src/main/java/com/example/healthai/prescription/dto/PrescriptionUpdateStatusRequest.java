package com.example.healthai.prescription.dto;

import jakarta.validation.constraints.NotBlank;

public record PrescriptionUpdateStatusRequest(
    @NotBlank(message = "目标状态不能为空")
    String status,
    String reason
) {
}
