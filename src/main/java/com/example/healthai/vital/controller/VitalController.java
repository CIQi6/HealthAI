package com.example.healthai.vital.controller;

import com.example.healthai.common.api.ApiResponse;
import com.example.healthai.vital.service.VitalService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vitals")
public class VitalController {

    private final VitalService service;

    public VitalController(VitalService service) {
        this.service = service;
    }

    @GetMapping("/probe")
    public ApiResponse<String> probe() {
        return ApiResponse.success(service.healthProbe());
    }
}
