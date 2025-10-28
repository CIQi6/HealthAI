package com.example.healthai.consult.controller;

import com.example.healthai.common.api.ApiResponse;
import com.example.healthai.consult.service.ConsultationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/consultations")
public class ConsultationController {

    private final ConsultationService consultationService;

    public ConsultationController(ConsultationService consultationService) {
        this.consultationService = consultationService;
    }

    @GetMapping("/probe")
    public ApiResponse<String> probe() {
        return ApiResponse.success(consultationService.healthProbe());
    }
}
