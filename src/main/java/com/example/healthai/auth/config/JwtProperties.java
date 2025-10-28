package com.example.healthai.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "healthai.security.jwt")
public record JwtProperties(
        String secret,
        long expireMinutes,
        String issuer
) {
}
