package com.example.healthai.drug.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.healthai.common.api.PageResponse;
import com.example.healthai.common.exception.BusinessException;
import com.example.healthai.common.exception.ErrorCode;
import com.example.healthai.drug.domain.Medicine;
import com.example.healthai.drug.dto.MedicineCreateRequest;
import com.example.healthai.drug.dto.MedicineResponse;
import com.example.healthai.drug.dto.MedicineUpdateRequest;
import com.example.healthai.drug.mapper.MedicineMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class DrugService {

    private static final int MAX_PAGE_SIZE = 100;

    private final MedicineMapper medicineMapper;
    private final ObjectMapper objectMapper;

    public DrugService(MedicineMapper medicineMapper, ObjectMapper objectMapper) {
        this.medicineMapper = medicineMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public MedicineResponse create(MedicineCreateRequest request) {
        medicineMapper.findByGenericName(request.genericName())
            .ifPresent(existing -> {
                throw new BusinessException(ErrorCode.DRUG_GENERIC_NAME_CONFLICT, "通用名已存在");
            });

        LocalDateTime now = LocalDateTime.now();
        Medicine medicine = Medicine.builder()
            .genericName(request.genericName())
            .brandName(request.brandName())
            .indications(request.indications())
            .contraindications(writeJson(request.contraindications()))
            .dosageGuideline(writeJson(request.dosageGuideline()))
            .drugInteractions(writeJson(request.drugInteractions()))
            .tags(writeJson(request.tags()))
            .version(1)
            .createdAt(now)
            .updatedAt(now)
            .build();
        medicineMapper.insert(medicine);
        return toResponse(medicine);
    }

    @Transactional
    public MedicineResponse update(Long id, MedicineUpdateRequest request) {
        Medicine existing = medicineMapper.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.DRUG_NOT_FOUND, "药品不存在"));

        if (!existing.getVersion().equals(request.version())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "药品数据已被他人修改，请刷新后重试");
        }

        medicineMapper.findByGenericName(request.genericName())
            .filter(other -> !other.getId().equals(id))
            .ifPresent(other -> {
                throw new BusinessException(ErrorCode.DRUG_GENERIC_NAME_CONFLICT, "通用名已存在");
            });

        existing.setGenericName(request.genericName());
        existing.setBrandName(request.brandName());
        existing.setIndications(request.indications());
        existing.setContraindications(writeJson(request.contraindications()));
        existing.setDosageGuideline(writeJson(request.dosageGuideline()));
        existing.setDrugInteractions(writeJson(request.drugInteractions()));
        existing.setTags(writeJson(request.tags()));
        existing.setVersion(existing.getVersion() + 1);
        existing.setUpdatedAt(LocalDateTime.now());

        medicineMapper.update(existing);
        return toResponse(existing);
    }

    @Transactional
    public void delete(Long id) {
        Medicine medicine = medicineMapper.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.DRUG_NOT_FOUND, "药品不存在"));
        int affected = medicineMapper.delete(medicine.getId());
        if (affected == 0) {
            throw new BusinessException(ErrorCode.DRUG_NOT_FOUND, "药品不存在");
        }
    }

    @Transactional(readOnly = true)
    public MedicineResponse detail(Long id) {
        Medicine medicine = medicineMapper.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.DRUG_NOT_FOUND, "药品不存在"));
        return toResponse(medicine);
    }

    @Transactional(readOnly = true)
    public PageResponse<MedicineResponse> search(String keyword, String contraindication, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int offset = safePage * safeSize;

        List<MedicineResponse> items = medicineMapper.search(keyword, contraindication, safeSize, offset).stream()
            .map(this::toResponse)
            .toList();
        long total = medicineMapper.count(keyword, contraindication);
        return new PageResponse<>(items, total, safePage, safeSize);
    }

    public String healthProbe() {
        return "drug-service-ok";
    }

    private String writeJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "JSON 字段序列化失败", ex);
        }
    }

    private JsonNode readJson(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(value);
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "药品 JSON 字段解析失败", ex);
        }
    }

    private MedicineResponse toResponse(Medicine medicine) {
        return new MedicineResponse(
            medicine.getId(),
            medicine.getGenericName(),
            medicine.getBrandName(),
            medicine.getIndications(),
            readJson(medicine.getContraindications()),
            readJson(medicine.getDosageGuideline()),
            readJson(medicine.getDrugInteractions()),
            readJson(medicine.getTags()),
            medicine.getVersion(),
            medicine.getCreatedAt(),
            medicine.getUpdatedAt()
        );
    }
}
