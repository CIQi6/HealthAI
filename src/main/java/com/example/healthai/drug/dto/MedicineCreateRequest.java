package com.example.healthai.drug.dto;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.validation.constraints.NotBlank;

public record MedicineCreateRequest(
    @NotBlank(message = "通用名不能为空")
    String genericName,
    String brandName,
    String indications,
    JsonNode contraindications,
    JsonNode dosageGuideline,
    JsonNode drugInteractions,
    JsonNode tags
) {
}
