package com.example.healthai.consult.controller;

import com.example.healthai.common.api.ApiResponse;
import com.example.healthai.consult.service.ConsultationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/consultations")
@Tag(name = "Consultation", description = "问诊模块接口")
public class ConsultationController {

    private final ConsultationService consultationService;

    public ConsultationController(ConsultationService consultationService) {
        this.consultationService = consultationService;
    }

    @GetMapping("/probe")
    @Operation(summary = "Consultation 模块健康探针")
    public ApiResponse<String> probe() {
        return ApiResponse.success(consultationService.healthProbe());
    }
}
