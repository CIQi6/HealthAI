package com.example.healthai.drug.controller;

import com.example.healthai.common.api.ApiResponse;
import com.example.healthai.common.api.PageResponse;
import com.example.healthai.drug.dto.MedicineCreateRequest;
import com.example.healthai.drug.dto.MedicineResponse;
import com.example.healthai.drug.dto.MedicineUpdateRequest;
import com.example.healthai.drug.service.DrugService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/drugs")
public class DrugController {

    private final DrugService drugService;

    public DrugController(DrugService drugService) {
        this.drugService = drugService;
    }

    @GetMapping("/probe")
    public ApiResponse<String> probe() {
        return ApiResponse.success(drugService.healthProbe());
    }

    @GetMapping
    public ApiResponse<PageResponse<MedicineResponse>> search(@RequestParam(required = false) String keyword,
                                                              @RequestParam(required = false) String contraindication,
                                                              @RequestParam(defaultValue = "0") int page,
                                                              @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(drugService.search(keyword, contraindication, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<MedicineResponse> detail(@PathVariable Long id) {
        return ApiResponse.success(drugService.detail(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MedicineResponse> create(@Valid @RequestBody MedicineCreateRequest request) {
        return ApiResponse.success(drugService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<MedicineResponse> update(@PathVariable Long id,
                                                @Valid @RequestBody MedicineUpdateRequest request) {
        return ApiResponse.success(drugService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        drugService.delete(id);
    }
}
