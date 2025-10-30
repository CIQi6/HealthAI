package com.example.healthai.drug.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.JsonNode;

public record MedicineResponse(
    Long id,
    String genericName,
    String brandName,
    String indications,
    JsonNode contraindications,
    JsonNode dosageGuideline,
    JsonNode drugInteractions,
    JsonNode tags,
    Integer version,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
