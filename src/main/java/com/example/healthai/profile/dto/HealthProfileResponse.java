package com.example.healthai.profile.dto;

import java.time.LocalDate;

public record HealthProfileResponse(
        Long id,
        Long userId,
        LocalDate birthDate,
        String bloodType,
        String chronicDiseases,
        String allergyHistory,
        String geneticRisk
) {
}
