package com.example.healthai.auth.dto;

import java.time.LocalDateTime;

import com.example.healthai.auth.domain.UserType;

public record UserProfileResponse(
        Long id,
        String username,
        String fullName,
        String gender,
        String phone,
        String email,
        UserType userType,
        LocalDateTime registeredAt,
        LocalDateTime lastLoginAt
) {
}
