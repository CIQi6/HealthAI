package com.example.healthai.security;

public interface SecretManagerClient {

    /**
     * Load secret value from external secret manager.
     *
     * @param secretKey identifier of the secret
     * @return secret value if available, otherwise {@code null}
     */
    String loadJwtSecret(String secretKey);
}
