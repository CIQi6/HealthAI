package com.example.healthai.prompt.dto;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.example.healthai.prompt.domain.PromptChannel;

public record PromptTemplateRequest(
        @NotBlank String code,
        @NotBlank String version,
        @NotNull PromptChannel channel,
        @NotBlank String modelName,
        @NotBlank String language,
        String description,
        @NotBlank String content,
        Map<String, Object> variables,
        Boolean enabled
) {
}
