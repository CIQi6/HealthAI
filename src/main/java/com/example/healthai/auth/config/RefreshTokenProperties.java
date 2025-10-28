package com.example.healthai.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "healthai.security.refresh-token")
public class RefreshTokenProperties {

    private long expireMinutes = 14 * 24 * 60; // 默认 14 天

    public long getExpireMinutes() {
        return expireMinutes;
    }

    public void setExpireMinutes(long expireMinutes) {
        this.expireMinutes = expireMinutes;
    }
}
