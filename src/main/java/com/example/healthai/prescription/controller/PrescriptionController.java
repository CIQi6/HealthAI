package com.example.healthai.prescription.controller;

import com.example.healthai.common.api.ApiResponse;
import com.example.healthai.common.api.PageResponse;
import com.example.healthai.prescription.dto.PrescriptionCreateRequest;
import com.example.healthai.prescription.dto.PrescriptionDetailResponse;
import com.example.healthai.prescription.dto.PrescriptionListResponse;
import com.example.healthai.prescription.dto.PrescriptionUpdateStatusRequest;
import com.example.healthai.prescription.service.PrescriptionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/prescriptions")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    public PrescriptionController(PrescriptionService prescriptionService) {
        this.prescriptionService = prescriptionService;
    }

    @GetMapping("/probe")
    public ApiResponse<String> probe() {
        return ApiResponse.success(prescriptionService.healthProbe());
    }

    @GetMapping
    public ApiResponse<PageResponse<PrescriptionListResponse.PrescriptionSummary>> search(
        @RequestParam(required = false) Long consultationId,
        @RequestParam(required = false) Long patientId,
        @RequestParam(required = false) Long doctorId,
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(prescriptionService.search(consultationId, patientId, doctorId, status, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<PrescriptionDetailResponse> detail(@PathVariable Long id) {
        return ApiResponse.success(prescriptionService.detail(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PrescriptionDetailResponse> create(@Valid @RequestBody PrescriptionCreateRequest request) {
        return ApiResponse.success(prescriptionService.create(request));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<PrescriptionDetailResponse> updateStatus(@PathVariable Long id,
                                                                @Valid @RequestBody PrescriptionUpdateStatusRequest request) {
        return ApiResponse.success(prescriptionService.updateStatus(id, request));
    }
}
