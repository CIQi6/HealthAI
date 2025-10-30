package com.example.healthai.drug.dto;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MedicineUpdateRequest(
    @NotBlank(message = "通用名不能为空")
    String genericName,
    String brandName,
    String indications,
    JsonNode contraindications,
    JsonNode dosageGuideline,
    JsonNode drugInteractions,
    JsonNode tags,
    @NotNull(message = "版本号不能为空")
    Integer version
) {
}
