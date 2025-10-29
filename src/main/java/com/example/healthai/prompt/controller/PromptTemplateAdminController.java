package com.example.healthai.prompt.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.healthai.common.api.ApiResponse;
import com.example.healthai.prompt.dto.PromptTemplateRequest;
import com.example.healthai.prompt.dto.PromptTemplateResponse;
import com.example.healthai.prompt.dto.PromptTemplateStatusRequest;
import com.example.healthai.prompt.service.PromptTemplateAdminService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/prompt-templates")
public class PromptTemplateAdminController {

    private final PromptTemplateAdminService promptTemplateAdminService;

    public PromptTemplateAdminController(PromptTemplateAdminService promptTemplateAdminService) {
        this.promptTemplateAdminService = promptTemplateAdminService;
    }

    @GetMapping
    public ApiResponse<List<PromptTemplateResponse>> list(@RequestParam(required = false) Boolean includeDisabled) {
        return ApiResponse.success(promptTemplateAdminService.list(includeDisabled));
    }

    @GetMapping("/{id}")
    public ApiResponse<PromptTemplateResponse> detail(@PathVariable Long id) {
        return ApiResponse.success(promptTemplateAdminService.detail(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PromptTemplateResponse> create(@Valid @RequestBody PromptTemplateRequest request) {
        return ApiResponse.success(promptTemplateAdminService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<PromptTemplateResponse> update(@PathVariable Long id,
                                                      @Valid @RequestBody PromptTemplateRequest request) {
        return ApiResponse.success(promptTemplateAdminService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<PromptTemplateResponse> updateStatus(@PathVariable Long id,
                                                            @Valid @RequestBody PromptTemplateStatusRequest request) {
        return ApiResponse.success(promptTemplateAdminService.updateStatus(id, request.enabled()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> delete(@PathVariable Long id) {
        promptTemplateAdminService.delete(id);
        return ApiResponse.success(null);
    }
}
