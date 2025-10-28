package com.example.healthai.auth.dto;

public record AuthResponse(
        String accessToken,
        long accessTokenExpiresInSeconds,
        String refreshToken,
        long refreshTokenExpiresInSeconds
) {
}
