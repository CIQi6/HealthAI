package com.example.healthai.notification.controller;

import com.example.healthai.common.api.ApiResponse;
import com.example.healthai.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notification", description = "通知与告警模块接口")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/probe")
    @Operation(summary = "Notification 模块健康探针")
    public ApiResponse<String> probe() {
        return ApiResponse.success(notificationService.healthProbe());
    }
}
