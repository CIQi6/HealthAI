package com.example.healthai.prompt.dto;

import java.time.LocalDateTime;
import java.util.Map;

import com.example.healthai.prompt.domain.PromptChannel;

public record PromptTemplateResponse(
        Long id,
        String code,
        String version,
        PromptChannel channel,
        String modelName,
        String language,
        String description,
        String content,
        Map<String, Object> variables,
        boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
