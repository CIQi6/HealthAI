package com.example.healthai.prompt.dto;

import jakarta.validation.constraints.NotNull;

public record PromptTemplateStatusRequest(
        @NotNull Boolean enabled
) {
}
