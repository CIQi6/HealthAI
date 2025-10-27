package com.example.healthai.drug.service;

import org.springframework.stereotype.Service;

@Service
public class DrugService {

    public String healthProbe() {
        return "drug-service-ok";
    }
}
