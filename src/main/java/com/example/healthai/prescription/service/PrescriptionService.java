package com.example.healthai.prescription.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.example.healthai.audit.AuditConstants;
import com.example.healthai.audit.service.AuditTrailService;
import com.example.healthai.common.api.PageResponse;
import com.example.healthai.common.exception.BusinessException;
import com.example.healthai.common.exception.ErrorCode;
import com.example.healthai.consult.domain.Consultation;
import com.example.healthai.consult.mapper.ConsultationMapper;
import com.example.healthai.drug.domain.Medicine;
import com.example.healthai.drug.mapper.MedicineMapper;
import com.example.healthai.notification.service.AlertingService;
import com.example.healthai.prescription.contra.ContraindicationReport;
import com.example.healthai.prescription.contra.ContraindicationViolation;
import com.example.healthai.prescription.contra.ContraindicationEvaluator;
import com.example.healthai.prescription.domain.ContraindicationAudit;
import com.example.healthai.prescription.domain.Prescription;
import com.example.healthai.prescription.domain.PrescriptionContraStatus;
import com.example.healthai.prescription.domain.PrescriptionItem;
import com.example.healthai.prescription.domain.PrescriptionStatus;
import com.example.healthai.prescription.dto.PrescriptionAuditResponse;
import com.example.healthai.prescription.dto.PrescriptionCreateRequest;
import com.example.healthai.prescription.dto.PrescriptionDetailResponse;
import com.example.healthai.prescription.dto.PrescriptionItemRequest;
import com.example.healthai.prescription.dto.PrescriptionItemResponse;
import com.example.healthai.prescription.dto.PrescriptionListResponse;
import com.example.healthai.prescription.dto.PrescriptionUpdateStatusRequest;
import com.example.healthai.prescription.mapper.ContraindicationAuditMapper;
import com.example.healthai.prescription.mapper.PrescriptionItemMapper;
import com.example.healthai.prescription.mapper.PrescriptionMapper;
import com.example.healthai.profile.domain.HealthProfile;
import com.example.healthai.profile.mapper.HealthProfileMapper;

@Service
public class PrescriptionService {

    private static final int MAX_PAGE_SIZE = 100;

    private final PrescriptionMapper prescriptionMapper;
    private final PrescriptionItemMapper prescriptionItemMapper;
    private final ContraindicationAuditMapper auditMapper;
    private final ConsultationMapper consultationMapper;
    private final HealthProfileMapper healthProfileMapper;
    private final MedicineMapper medicineMapper;
    private final ContraindicationEvaluator contraEvaluator;
    private final AuditTrailService auditTrailService;
    private final AlertingService alertingService;
    private final ObjectMapper objectMapper;

    public PrescriptionService(PrescriptionMapper prescriptionMapper,
                               PrescriptionItemMapper prescriptionItemMapper,
                               ContraindicationAuditMapper auditMapper,
                               ConsultationMapper consultationMapper,
                               HealthProfileMapper healthProfileMapper,
                               MedicineMapper medicineMapper,
                               ContraindicationEvaluator contraEvaluator,
                               AuditTrailService auditTrailService,
                               AlertingService alertingService,
                               ObjectMapper objectMapper) {
        this.prescriptionMapper = prescriptionMapper;
        this.prescriptionItemMapper = prescriptionItemMapper;
        this.auditMapper = auditMapper;
        this.consultationMapper = consultationMapper;
        this.healthProfileMapper = healthProfileMapper;
        this.medicineMapper = medicineMapper;
        this.contraEvaluator = contraEvaluator;
        this.auditTrailService = auditTrailService;
        this.alertingService = alertingService;
        this.objectMapper = objectMapper;
    }

    public String healthProbe() {
        return "prescription-service-ok";
    }

    @Transactional
    public PrescriptionDetailResponse create(PrescriptionCreateRequest request) {
        Consultation consultation = consultationMapper.findById(request.consultationId())
            .orElseThrow(() -> new BusinessException(ErrorCode.CONSULTATION_NOT_FOUND, "问诊不存在"));

        HealthProfile profile = healthProfileMapper.findByUserId(consultation.getUserId())
            .orElse(null);

        LocalDateTime now = LocalDateTime.now();
        Prescription prescription = Prescription.builder()
            .consultationId(consultation.getId())
            .patientId(consultation.getUserId())
            .doctorId(request.doctorId())
            .status(PrescriptionStatus.DRAFT)
            .contraCheckStatus(PrescriptionContraStatus.PASS)
            .notes(request.notes())
            .createdAt(now)
            .updatedAt(now)
            .build();
        prescriptionMapper.insert(prescription);

        Map<Long, Medicine> medicineById = loadMedicines(request.items());
        List<PrescriptionItem> items = buildItems(prescription.getId(), request.items(), medicineById, now);
        items.forEach(prescriptionItemMapper::insert);
        List<PrescriptionItem> persistedItems = prescriptionItemMapper.findByPrescriptionId(prescription.getId());

        ContraindicationReport report = evaluateAndAudit(prescription, persistedItems, medicineById, profile);

        if (report.status() == PrescriptionContraStatus.FAIL) {
            alertingService.notifyContraindicationFailure(prescription.getId(), report);
            throw new BusinessException(ErrorCode.DRUG_CONTRAINDICATED, report.summary());
        }

        prescription.setContraCheckStatus(report.status());
        prescription.setContraFailReason(report.summary());
        prescription.setUpdatedAt(LocalDateTime.now());
        prescriptionMapper.update(prescription);

        auditTrailService.recordConsultationEvent(
            AuditConstants.ACTION_CONSULT_REVIEWED,
            request.doctorId(),
            "DOCTOR",
            consultation.getId().toString(),
            null,
            "PRESCRIPTION_CREATED");

        return buildDetailResponse(prescription, persistedItems, report.violations(), medicineById);
    }

    @Transactional(readOnly = true)
    public PrescriptionDetailResponse detail(Long id) {
        Prescription prescription = prescriptionMapper.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.PRESCRIPTION_NOT_FOUND, "处方不存在"));
        List<PrescriptionItem> items = prescriptionItemMapper.findByPrescriptionId(id);
        Map<Long, Medicine> medicineById = loadMedicines(items.stream()
            .map(item -> new PrescriptionItemRequest(item.getMedicineId(), item.getDosageInstruction(), item.getFrequency(), item.getDaySupply(), item.getQuantity()))
            .toList());
        List<ContraindicationAudit> audits = auditMapper.findByPrescriptionId(id);
        return buildDetailResponse(prescription, items, audits, medicineById);
    }

    @Transactional(readOnly = true)
    public PageResponse<PrescriptionListResponse.PrescriptionSummary> search(Long consultationId,
                                                                             Long patientId,
                                                                             Long doctorId,
                                                                             String status,
                                                                             int page,
                                                                             int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int offset = safePage * safeSize;

        PrescriptionStatus statusFilter = parseStatus(status);

        List<PrescriptionListResponse.PrescriptionSummary> items = prescriptionMapper.search(consultationId, patientId, doctorId, statusFilter, safeSize, offset)
            .stream()
            .map(this::toSummary)
            .toList();
        long total = prescriptionMapper.count(consultationId, patientId, doctorId, statusFilter);

        return new PageResponse<>(items, total, safePage, safeSize);
    }

    @Transactional
    public PrescriptionDetailResponse updateStatus(Long id, PrescriptionUpdateStatusRequest request) {
        Prescription prescription = prescriptionMapper.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.PRESCRIPTION_NOT_FOUND, "处方不存在"));

        PrescriptionStatus newStatus = parseStatusMandatory(request.status());
        if (prescription.getStatus() == PrescriptionStatus.CANCELLED || prescription.getStatus() == PrescriptionStatus.ISSUED) {
            throw new BusinessException(ErrorCode.PRESCRIPTION_STATUS_CONFLICT, "当前处方状态不允许此操作");
        }

        if (newStatus == PrescriptionStatus.ISSUED && prescription.getContraCheckStatus() == PrescriptionContraStatus.FAIL) {
            throw new BusinessException(ErrorCode.DRUG_CONTRAINDICATED, "禁忌校验未通过，无法签发");
        }

        LocalDateTime now = LocalDateTime.now();
        prescriptionMapper.updateStatus(id, newStatus, prescription.getContraCheckStatus(), prescription.getContraFailReason(), now);

        auditTrailService.recordConsultationEvent(
            "PRESCRIPTION_STATUS_CHANGED",
            prescription.getDoctorId(),
            "DOCTOR",
            prescription.getConsultationId().toString(),
            null,
            newStatus.name());

        return detail(id);
    }

    private Map<Long, Medicine> loadMedicines(List<PrescriptionItemRequest> requests) {
        if (CollectionUtils.isEmpty(requests)) {
            return Map.of();
        }
        return requests.stream()
            .map(PrescriptionItemRequest::drugId)
            .distinct()
            .map(id -> medicineMapper.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DRUG_NOT_FOUND, "药品不存在")))
            .collect(Collectors.toMap(Medicine::getId, medicine -> medicine, (left, right) -> left));
    }

    private List<PrescriptionItem> buildItems(Long prescriptionId,
                                              List<PrescriptionItemRequest> requests,
                                              Map<Long, Medicine> medicineById,
                                              LocalDateTime now) {
        if (CollectionUtils.isEmpty(requests)) {
            return List.of();
        }
        List<PrescriptionItem> items = new ArrayList<>(requests.size());
        for (PrescriptionItemRequest request : requests) {
            Medicine medicine = medicineById.get(request.drugId());
            if (medicine == null) {
                throw new BusinessException(ErrorCode.DRUG_NOT_FOUND, "药品不存在");
            }
            PrescriptionItem item = PrescriptionItem.builder()
                .prescriptionId(prescriptionId)
                .medicineId(medicine.getId())
                .dosageInstruction(request.dosageInstruction())
                .frequency(request.frequency())
                .daySupply(request.daySupply())
                .quantity(request.quantity())
                .contraResult(PrescriptionContraStatus.PASS)
                .createdAt(now)
                .updatedAt(now)
                .build();
            items.add(item);
        }
        return items;
    }

    private ContraindicationReport evaluateAndAudit(Prescription prescription,
                                                    List<PrescriptionItem> items,
                                                    Map<Long, Medicine> medicineById,
                                                    HealthProfile profile) {
        ContraindicationReport report = contraEvaluator.evaluate(prescription, items, medicineById, profile);
        LocalDateTime now = LocalDateTime.now();
        Set<Long> updatedItemIds = new HashSet<>();

        for (ContraindicationViolation violation : report.violations()) {
            PrescriptionItem item = items.get(violation.itemIndex());
            PrescriptionContraStatus mergedLevel = mergeLevel(item.getContraResult(), violation.level());
            if (mergedLevel != item.getContraResult()) {
                item.setContraResult(mergedLevel);
                item.setUpdatedAt(now);
                updatedItemIds.add(item.getId());
            }
            String violationPayload = serializeViolation(violation, medicineById);
            ContraindicationAudit audit = ContraindicationAudit.builder()
                .prescriptionId(prescription.getId())
                .prescriptionItemId(item.getId())
                .checkTime(now)
                .checker("SYSTEM")
                .patientSnapshot(serializeSnapshot(profile))
                .violations(violationPayload)
                .result(violation.level())
                .message(violation.message())
                .createdAt(now)
                .updatedAt(now)
                .build();
            auditMapper.insert(audit);
        }

        if (!updatedItemIds.isEmpty()) {
            for (PrescriptionItem item : items) {
                if (updatedItemIds.contains(item.getId())) {
                    prescriptionItemMapper.update(item);
                }
            }
        }
        return report;
    }

    private String serializeSnapshot(HealthProfile profile) {
        if (profile == null) {
            return null;
        }
        try {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("allergyHistory", safe(profile.getAllergyHistory()));
            node.put("chronicDiseases", safe(profile.getChronicDiseases()));
            node.put("geneticRisk", safe(profile.getGeneticRisk()));
            node.put("bloodType", safe(profile.getBloodType()));
            return objectMapper.writeValueAsString(node);
        } catch (Exception ex) {
            return "allergy=" + safe(profile.getAllergyHistory()) + ";chronic=" + safe(profile.getChronicDiseases());
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String serializeViolation(ContraindicationViolation violation, Map<Long, Medicine> medicineById) {
        try {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("itemIndex", violation.itemIndex());
            node.put("medicineId", violation.medicineId());
            node.put("type", violation.type());
            node.put("level", violation.level().name());
            node.put("message", violation.message());
            Medicine medicine = medicineById.get(violation.medicineId());
            if (medicine != null) {
                node.put("genericName", safe(medicine.getGenericName()));
                node.put("brandName", safe(medicine.getBrandName()));
            }
            return objectMapper.writeValueAsString(node);
        } catch (Exception ex) {
            return violation.message();
        }
    }

    private PrescriptionContraStatus mergeLevel(PrescriptionContraStatus left, PrescriptionContraStatus right) {
        if (left == null) {
            return right == null ? PrescriptionContraStatus.PASS : right;
        }
        if (right == null) {
            return left;
        }
        if (left == PrescriptionContraStatus.FAIL || right == PrescriptionContraStatus.FAIL) {
            return PrescriptionContraStatus.FAIL;
        }
        if (left == PrescriptionContraStatus.WARN || right == PrescriptionContraStatus.WARN) {
            return PrescriptionContraStatus.WARN;
        }
        return PrescriptionContraStatus.PASS;
    }

    private PrescriptionListResponse.PrescriptionSummary toSummary(Prescription prescription) {
        return new PrescriptionListResponse.PrescriptionSummary(
            prescription.getId(),
            prescription.getConsultationId(),
            prescription.getPatientId(),
            prescription.getDoctorId(),
            prescription.getStatus().name(),
            prescription.getContraCheckStatus().name(),
            formatDate(prescription.getCreatedAt()),
            formatDate(prescription.getUpdatedAt())
        );
    }

    private PrescriptionDetailResponse buildDetailResponse(Prescription prescription,
                                                           List<PrescriptionItem> items,
                                                           List<? extends Object> audits,
                                                           Map<Long, Medicine> medicineById) {
        List<PrescriptionItemResponse> itemResponses = items.stream()
            .map(item -> toItemResponse(item, medicineById))
            .toList();
        List<PrescriptionAuditResponse> auditResponses = audits.stream()
            .map(this::toAuditResponse)
            .collect(Collectors.toList());
        return new PrescriptionDetailResponse(
            prescription.getId(),
            prescription.getConsultationId(),
            prescription.getPatientId(),
            prescription.getDoctorId(),
            prescription.getStatus().name(),
            prescription.getContraCheckStatus().name(),
            prescription.getContraFailReason(),
            prescription.getNotes(),
            formatDate(prescription.getCreatedAt()),
            formatDate(prescription.getUpdatedAt()),
            itemResponses,
            auditResponses
        );
    }

    private PrescriptionItemResponse toItemResponse(PrescriptionItem item,
                                                    Map<Long, Medicine> medicineById) {
        Medicine medicine = medicineById.get(item.getMedicineId());
        if (medicine == null) {
            medicine = medicineMapper.findById(item.getMedicineId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DRUG_NOT_FOUND, "药品不存在"));
            medicineById.put(medicine.getId(), medicine);
        }
        return new PrescriptionItemResponse(
            item.getId(),
            item.getMedicineId(),
            medicine.getGenericName(),
            medicine.getBrandName(),
            item.getDosageInstruction(),
            item.getFrequency(),
            item.getDaySupply(),
            item.getQuantity(),
            item.getContraResult().name(),
            medicine.getContraindications()
        );
    }

    private PrescriptionAuditResponse toAuditResponse(Object auditObject) {
        if (auditObject instanceof ContraindicationAudit audit) {
            return new PrescriptionAuditResponse(
                audit.getId(),
                audit.getPrescriptionItemId(),
                audit.getChecker(),
                audit.getResult().name(),
                audit.getMessage(),
                audit.getViolations(),
                formatDate(audit.getCheckTime())
            );
        }
        if (auditObject instanceof ContraindicationViolation violation) {
            return new PrescriptionAuditResponse(
                null,
                null,
                "SYSTEM",
                violation.level().name(),
                violation.message(),
                violation.message(),
                formatDate(LocalDateTime.now())
            );
        }
        throw new IllegalArgumentException("Unsupported audit object type: " + auditObject.getClass());
    }

    private PrescriptionStatus parseStatusMandatory(String status) {
        if (!StringUtils.hasText(status)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "状态不能为空");
        }
        return parseStatus(status);
    }

    private PrescriptionStatus parseStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        try {
            return PrescriptionStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "无效的处方状态");
        }
    }

    private String formatDate(LocalDateTime timestamp) {
        return timestamp == null ? null : timestamp.toString();
    }
}
