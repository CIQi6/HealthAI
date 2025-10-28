package com.example.healthai.profile.controller;

import com.example.healthai.common.api.ApiResponse;
import com.example.healthai.profile.service.HealthProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health-profiles")
@Tag(name = "Health Profile", description = "健康档案模块接口")
public class HealthProfileController {

    private final HealthProfileService service;

    public HealthProfileController(HealthProfileService service) {
        this.service = service;
    }

    @GetMapping("/probe")
    @Operation(summary = "Health Profile 模块健康探针")
    public ApiResponse<String> probe() {
        return ApiResponse.success(service.healthProbe());
    }
}
