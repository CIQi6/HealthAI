package com.example.healthai.prescription.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PrescriptionCreateRequest(
    @NotNull(message = "问诊ID不能为空")
    Long consultationId,
    @NotNull(message = "医生ID不能为空")
    Long doctorId,
    String notes,
    @Valid
    @Size(min = 1, message = "处方至少包含一个药品")
    List<PrescriptionItemRequest> items
) {
}
