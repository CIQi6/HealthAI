package com.example.healthai.auth.controller;

import com.example.healthai.auth.service.AuthService;
import com.example.healthai.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "用户账号与认证接口")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/probe")
    @Operation(summary = "Auth 模块健康探针")
    public ApiResponse<String> probe() {
        return ApiResponse.success(authService.healthProbe());
    }
}
