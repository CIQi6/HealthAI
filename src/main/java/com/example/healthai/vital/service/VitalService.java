package com.example.healthai.vital.service;

import org.springframework.stereotype.Service;

@Service
public class VitalService {

    public String healthProbe() {
        return "vital-service-ok";
    }
}
