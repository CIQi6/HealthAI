package com.example.healthai.drug.controller;

import com.example.healthai.common.api.ApiResponse;
import com.example.healthai.drug.service.DrugService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
