package com.example.healthai.prescription.controller;

import com.example.healthai.common.api.ApiResponse;
import com.example.healthai.prescription.service.PrescriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/prescriptions")
@Tag(name = "Prescription", description = "处方模块接口")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    public PrescriptionController(PrescriptionService prescriptionService) {
        this.prescriptionService = prescriptionService;
    }

    @GetMapping("/probe")
    @Operation(summary = "Prescription 模块健康探针")
    public ApiResponse<String> probe() {
        return ApiResponse.success(prescriptionService.healthProbe());
    }
}
