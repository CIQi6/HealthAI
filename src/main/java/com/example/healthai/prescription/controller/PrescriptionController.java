package com.example.healthai.prescription.controller;

import com.example.healthai.common.api.ApiResponse;
import com.example.healthai.prescription.service.PrescriptionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/prescriptions")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    public PrescriptionController(PrescriptionService prescriptionService) {
        this.prescriptionService = prescriptionService;
    }

    @GetMapping("/probe")
    public ApiResponse<String> probe() {
        return ApiResponse.success(prescriptionService.healthProbe());
    }
}
