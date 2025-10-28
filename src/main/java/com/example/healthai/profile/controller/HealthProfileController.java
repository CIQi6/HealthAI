package com.example.healthai.profile.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.healthai.common.api.ApiResponse;
import com.example.healthai.profile.dto.HealthProfileRequest;
import com.example.healthai.profile.dto.HealthProfileResponse;
import com.example.healthai.profile.service.HealthProfileService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/health-profiles")
public class HealthProfileController {

    private final HealthProfileService service;

    public HealthProfileController(HealthProfileService service) {
        this.service = service;
    }

    @GetMapping("/probe")
    public ApiResponse<String> probe() {
        return ApiResponse.success(service.healthProbe());
    }

    @GetMapping
    public ApiResponse<HealthProfileResponse> currentProfile(Authentication authentication) {
        return ApiResponse.success(service.findByUsername(authentication.getName()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<HealthProfileResponse> createOrUpdate(@Valid @RequestBody HealthProfileRequest request,
                                                             Authentication authentication) {
        return ApiResponse.success(service.createOrUpdate(authentication.getName(), request));
    }

    @GetMapping("/{id}")
    public ApiResponse<HealthProfileResponse> findById(@PathVariable Long id, Authentication authentication) {
        return ApiResponse.success(service.findById(authentication.getName(), id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication authentication) {
        service.delete(authentication.getName(), id);
    }
}
