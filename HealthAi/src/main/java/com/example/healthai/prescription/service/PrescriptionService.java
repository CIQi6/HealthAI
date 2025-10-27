package com.example.healthai.prescription.service;

import org.springframework.stereotype.Service;

@Service
public class PrescriptionService {

    public String healthProbe() {
        return "prescription-service-ok";
    }
}
