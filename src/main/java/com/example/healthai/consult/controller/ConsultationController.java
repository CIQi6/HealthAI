package com.example.healthai.consult.controller;

import com.example.healthai.common.api.ApiResponse;
import com.example.healthai.consult.dto.ConsultationCreateRequest;
import com.example.healthai.consult.dto.ConsultationDetailResponse;
import com.example.healthai.consult.dto.ConsultationListResponse;
import com.example.healthai.consult.dto.ConsultationReviewRequest;
import com.example.healthai.consult.service.ConsultationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/consultations")
public class ConsultationController {

    private final ConsultationService consultationService;

    public ConsultationController(ConsultationService consultationService) {
        this.consultationService = consultationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ConsultationDetailResponse> create(@Valid @RequestBody ConsultationCreateRequest request,
                                                          Authentication authentication) {
        return ApiResponse.success(consultationService.create(authentication.getName(), request));
    }

    @GetMapping
    public ApiResponse<ConsultationListResponse> list(@RequestParam(required = false) String status,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "20") int size,
                                                      Authentication authentication) {
        return ApiResponse.success(consultationService.list(authentication.getName(), status, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<ConsultationDetailResponse> detail(@PathVariable Long id, Authentication authentication) {
        return ApiResponse.success(consultationService.detail(authentication.getName(), id));
    }

    @PostMapping("/{id}/review")
    public ApiResponse<ConsultationDetailResponse> review(@PathVariable Long id,
                                                          @Valid @RequestBody ConsultationReviewRequest request,
                                                          Authentication authentication) {
        return ApiResponse.success(consultationService.review(authentication.getName(), id, request));
    }

    @PostMapping("/{id}/close")
    public ApiResponse<ConsultationDetailResponse> close(@PathVariable Long id,
                                                         Authentication authentication) {
        return ApiResponse.success(consultationService.close(authentication.getName(), id));
    }
}
