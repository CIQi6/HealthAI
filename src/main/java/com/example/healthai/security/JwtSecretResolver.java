package com.example.healthai.security;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.example.healthai.auth.config.JwtProperties;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtSecretResolver {

    private final JwtProperties properties;
    private final SecretManagerClient secretManagerClient;
    private final AtomicReference<SecretKey> cachedSigningKey = new AtomicReference<>();

    public JwtSecretResolver(JwtProperties properties, SecretManagerClient secretManagerClient) {
        this.properties = properties;
        this.secretManagerClient = secretManagerClient;
    }

    public SecretKey getSigningKey() {
        SecretKey key = cachedSigningKey.get();
        if (key != null) {
            return key;
        }
        SecretKey resolved = resolveSigningKey();
        if (cachedSigningKey.compareAndSet(null, resolved)) {
            return resolved;
        }
        return cachedSigningKey.get();
    }

    private SecretKey resolveSigningKey() {
        String secretValue = resolveSecretValue();
        byte[] keyBytes = toKeyBytes(secretValue);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 256 bits long");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private String resolveSecretValue() {
        String secret = properties.getSecret();
        if (StringUtils.hasText(secret)) {
            return secret;
        }
        String secretKey = properties.getSecretKey();
        if (StringUtils.hasText(secretKey)) {
            String loaded = secretManagerClient.loadJwtSecret(secretKey);
            if (StringUtils.hasText(loaded)) {
                return loaded;
            }
        }
        throw new IllegalStateException("JWT secret is not configured. Provide 'healthai.security.jwt.secret' or a valid secret key reference.");
    }

    private byte[] toKeyBytes(String secret) {
        String trimmed = secret.trim();
        try {
            return Decoders.BASE64.decode(trimmed);
        } catch (DecodingException | IllegalArgumentException ex) {
            return trimmed.getBytes(StandardCharsets.UTF_8);
        }
    }
}
