package com.example.healthai.profile.service;

import org.springframework.stereotype.Service;

@Service
public class HealthProfileService {

    public String healthProbe() {
        return "health-profile-service-ok";
    }
}
