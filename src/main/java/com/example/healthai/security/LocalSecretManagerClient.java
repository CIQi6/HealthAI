package com.example.healthai.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class LocalSecretManagerClient implements SecretManagerClient {

    private static final Logger log = LoggerFactory.getLogger(LocalSecretManagerClient.class);

    private final Environment environment;

    public LocalSecretManagerClient(Environment environment) {
        this.environment = environment;
    }

    @Override
    public String loadJwtSecret(String secretKey) {
        if (!StringUtils.hasText(secretKey)) {
            return null;
        }
        String value = environment.getProperty(secretKey);
        if (!StringUtils.hasText(value)) {
            value = System.getenv(secretKey);
        }
        if (!StringUtils.hasText(value)) {
            log.warn("Secret key '{}' not found in environment or system properties", secretKey);
        }
        return value;
    }
}
