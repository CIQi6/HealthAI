package com.example.healthai.auth.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.example.healthai.auth.config.JwtProperties;
import com.example.healthai.security.JwtSecretResolver;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

@Component
public class JwtProvider {

    private final JwtProperties properties;
    private final JwtSecretResolver jwtSecretResolver;

    public JwtProvider(JwtProperties properties, JwtSecretResolver jwtSecretResolver) {
        this.properties = properties;
        this.jwtSecretResolver = jwtSecretResolver;
    }

    public String generateToken(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        String username;
        if (principal instanceof UserDetails userDetails) {
            username = userDetails.getUsername();
        } else {
            username = principal.toString();
        }
        return generateToken(username);
    }

    public String generateToken(String username) {
        Instant now = Instant.now();
        Instant expiry = now.plus(properties.getExpireMinutes(), ChronoUnit.MINUTES);

        return Jwts.builder()
                .subject(username)
                .issuer(properties.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(jwtSecretResolver.getSigningKey())
                .compact();
    }

    public Optional<String> validateAndGetSubject(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(jwtSecretResolver.getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.ofNullable(claims.getSubject());
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public long getExpirySeconds() {
        return properties.getExpireMinutes() * 60L;
    }
}
