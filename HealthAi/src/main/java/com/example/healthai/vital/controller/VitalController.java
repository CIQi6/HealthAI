package com.example.healthai.vital.controller;

import com.example.healthai.common.api.ApiResponse;
import com.example.healthai.vital.service.VitalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vitals")
@Tag(name = "Vital Signs", description = "生理指标模块接口")
public class VitalController {

    private final VitalService service;

    public VitalController(VitalService service) {
        this.service = service;
    }

    @GetMapping("/probe")
    @Operation(summary = "Vital 模块健康探针")
    public ApiResponse<String> probe() {
        return ApiResponse.success(service.healthProbe());
    }
}
