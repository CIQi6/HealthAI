package com.example.healthai.audit.controller;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.healthai.audit.dto.AuditEventResponse;
import com.example.healthai.audit.service.AuditEventAdminService;
import com.example.healthai.common.api.ApiResponse;
import com.example.healthai.common.api.PageResponse;

@RestController
@RequestMapping("/api/v1/admin/audit-events")
public class AuditEventAdminController {

    private final AuditEventAdminService auditEventAdminService;

    public AuditEventAdminController(AuditEventAdminService auditEventAdminService) {
        this.auditEventAdminService = auditEventAdminService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AuditEventResponse>> search(
        @RequestParam(required = false) String resourceType,
        @RequestParam(required = false) String action,
        @RequestParam(required = false) Long actorId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(
            auditEventAdminService.search(resourceType, action, actorId, from, to, page, size));
    }
}
