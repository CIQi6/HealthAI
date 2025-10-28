package com.example.healthai.auth.service;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.example.healthai.auth.config.RefreshTokenProperties;
import com.example.healthai.common.exception.BusinessException;
import com.example.healthai.common.exception.ErrorCode;

@Service
public class RefreshTokenService {

    private static final String KEY_PREFIX = "auth:refresh:";

    private final StringRedisTemplate redisTemplate;
    private final RefreshTokenProperties properties;

    private final Map<String, InMemoryToken> inMemoryStore = new java.util.concurrent.ConcurrentHashMap<>();

    public RefreshTokenService(StringRedisTemplate redisTemplate, RefreshTokenProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    public GeneratedRefreshToken create(String username) {
        if (!StringUtils.hasText(username)) {
            throw new IllegalArgumentException("username must not be blank");
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        long expireMinutes = properties.getExpireMinutes();
        long expireSeconds = TimeUnit.MINUTES.toSeconds(expireMinutes);
        String key = toKey(token);
        try {
            redisTemplate.opsForValue().set(key, username, Duration.ofMinutes(expireMinutes));
        } catch (Exception ex) {
            storeInMemory(key, username, expireSeconds);
        }
        return new GeneratedRefreshToken(token, expireSeconds);
    }

    public String consume(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID, "刷新令牌不能为空");
        }
        String key = toKey(refreshToken);
        String username = null;
        try {
            username = redisTemplate.opsForValue().get(key);
            if (StringUtils.hasText(username)) {
                redisTemplate.delete(key);
                return username;
            }
        } catch (Exception ex) {
            // fall back to in-memory store below
        }
        username = consumeFromMemory(key);
        if (!StringUtils.hasText(username)) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        return username;
    }

    public void revoke(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            return;
        }
        String key = toKey(refreshToken);
        try {
            redisTemplate.delete(key);
        } catch (Exception ex) {
            inMemoryStore.remove(key);
        }
    }

    public long getExpireSeconds() {
        return TimeUnit.MINUTES.toSeconds(properties.getExpireMinutes());
    }

    private String toKey(String token) {
        return KEY_PREFIX + token;
    }

    public record GeneratedRefreshToken(String token, long expiresInSeconds) {
    }

    private void storeInMemory(String key, String username, long expireSeconds) {
        long expiresAt = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(expireSeconds);
        inMemoryStore.put(key, new InMemoryToken(username, expiresAt));
    }

    private String consumeFromMemory(String key) {
        InMemoryToken token = inMemoryStore.remove(key);
        if (token == null) {
            return null;
        }
        if (System.currentTimeMillis() > token.expiresAt()) {
            return null;
        }
        return token.username();
    }

    private record InMemoryToken(String username, long expiresAt) {
    }
}
