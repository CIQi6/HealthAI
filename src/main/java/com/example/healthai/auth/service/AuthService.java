package com.example.healthai.auth.service;

import com.example.healthai.auth.repository.AuthMapper;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthMapper authMapper;

    public AuthService(AuthMapper authMapper) {
        this.authMapper = authMapper;
    }

    public String healthProbe() {
        Integer result = authMapper.ping();
        return result != null ? "auth-service-ok" : "auth-service-degraded";
    }
}
