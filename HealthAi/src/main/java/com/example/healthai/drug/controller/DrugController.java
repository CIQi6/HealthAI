package com.example.healthai.drug.controller;

import com.example.healthai.common.api.ApiResponse;
import com.example.healthai.drug.service.DrugService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/drugs")
@Tag(name = "Drug Catalog", description = "药品库模块接口")
public class DrugController {

    private final DrugService drugService;

    public DrugController(DrugService drugService) {
        this.drugService = drugService;
    }

    @GetMapping("/probe")
    @Operation(summary = "Drug 模块健康探针")
    public ApiResponse<String> probe() {
        return ApiResponse.success(drugService.healthProbe());
    }
}
