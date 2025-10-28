package com.example.healthai.consult.service;

import org.springframework.stereotype.Service;

@Service
public class ConsultationService {

    public String healthProbe() {
        return "consultation-service-ok";
    }
}
