package com.example.healthai.profile.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Pattern;

public record HealthProfileRequest(
        LocalDate birthDate,
        @Pattern(regexp = "^(A|B|AB|O|UNKNOWN)?$", message = "血型取值无效")
        String bloodType,
        String chronicDiseases,
        String allergyHistory,
        String geneticRisk
) {
}
