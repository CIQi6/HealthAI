package com.example.healthai.prescription.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.healthai.audit.service.AuditTrailService;
import com.example.healthai.common.api.PageResponse;
import com.example.healthai.common.exception.BusinessException;
import com.example.healthai.common.exception.ErrorCode;
import com.example.healthai.consult.domain.Consultation;
import com.example.healthai.consult.mapper.ConsultationMapper;
import com.example.healthai.drug.domain.Medicine;
import com.example.healthai.drug.mapper.MedicineMapper;
import com.example.healthai.notification.service.AlertingService;
import com.example.healthai.prescription.contra.ContraindicationEvaluator;
import com.example.healthai.prescription.contra.ContraindicationReport;
import com.example.healthai.prescription.contra.ContraindicationViolation;
import com.example.healthai.prescription.domain.ContraindicationAudit;
import com.example.healthai.prescription.domain.Prescription;
import com.example.healthai.prescription.domain.PrescriptionContraStatus;
import com.example.healthai.prescription.domain.PrescriptionItem;
import com.example.healthai.prescription.domain.PrescriptionStatus;
import com.example.healthai.prescription.dto.PrescriptionCreateRequest;
import com.example.healthai.prescription.dto.PrescriptionDetailResponse;
import com.example.healthai.prescription.dto.PrescriptionItemRequest;
import com.example.healthai.prescription.dto.PrescriptionListResponse;
import com.example.healthai.prescription.dto.PrescriptionUpdateStatusRequest;
import com.example.healthai.prescription.mapper.ContraindicationAuditMapper;
import com.example.healthai.prescription.mapper.PrescriptionItemMapper;
import com.example.healthai.prescription.mapper.PrescriptionMapper;
import com.example.healthai.profile.domain.HealthProfile;
import com.example.healthai.profile.mapper.HealthProfileMapper;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class PrescriptionServiceTest {

    @Mock
    private PrescriptionMapper prescriptionMapper;
    @Mock
    private PrescriptionItemMapper prescriptionItemMapper;
    @Mock
    private ContraindicationAuditMapper auditMapper;
    @Mock
    private ConsultationMapper consultationMapper;
    @Mock
    private HealthProfileMapper healthProfileMapper;
    @Mock
    private MedicineMapper medicineMapper;
    @Mock
    private ContraindicationEvaluator contraEvaluator;
    @Mock
    private AuditTrailService auditTrailService;
    @Mock
    private AlertingService alertingService;

    private PrescriptionService prescriptionService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        prescriptionService = new PrescriptionService(
            prescriptionMapper,
            prescriptionItemMapper,
            auditMapper,
            consultationMapper,
            healthProfileMapper,
            medicineMapper,
            contraEvaluator,
            auditTrailService,
            alertingService,
            objectMapper
        );
    }

    @Test
    void createShouldPersistItemsAndAuditsWhenContraPass() {
        LocalDateTime now = LocalDateTime.now();
        Consultation consultation = Consultation.builder()
            .id(1L)
            .userId(200L)
            .doctorId(300L)
            .status(com.example.healthai.consult.domain.ConsultationStatus.AI_REVIEWED)
            .createdAt(now)
            .updatedAt(now)
            .build();
        when(consultationMapper.findById(1L)).thenReturn(Optional.of(consultation));
        when(healthProfileMapper.findByUserId(200L)).thenReturn(Optional.of(HealthProfile.builder()
            .id(99L)
            .userId(200L)
            .chronicDiseases("hypertension")
            .createdAt(now)
            .updatedAt(now)
            .build()));

        doAnswer(invocation -> {
            Prescription prescription = invocation.getArgument(0);
            prescription.setId(10L);
            return 1;
        }).when(prescriptionMapper).insert(any(Prescription.class));

        Medicine medicine = medicine();
        when(medicineMapper.findById(11L)).thenReturn(Optional.of(medicine));

        doAnswer(invocation -> {
            PrescriptionItem item = invocation.getArgument(0);
            item.setId(101L);
            return 1;
        }).when(prescriptionItemMapper).insert(any(PrescriptionItem.class));

        when(prescriptionItemMapper.findByPrescriptionId(10L)).thenReturn(List.of(buildItem(101L)));
        when(contraEvaluator.evaluate(any(), any(), any(), any())).thenReturn(ContraindicationReport.pass());

        PrescriptionDetailResponse response = prescriptionService.create(new PrescriptionCreateRequest(
            1L,
            300L,
            "note",
            List.of(new PrescriptionItemRequest(11L, "Take one", "daily", 7, BigDecimal.ONE))
        ));

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.items()).hasSize(1);
        verify(prescriptionMapper).update(any(Prescription.class));
        verify(auditTrailService).recordConsultationEvent(any(), any(), any(), any(), any(), any());
    }

    @Test
    void createShouldThrowWhenContraFailAndSendAlert() {
        LocalDateTime now = LocalDateTime.now();
        Consultation consultation = Consultation.builder()
            .id(2L)
            .userId(201L)
            .doctorId(301L)
            .status(com.example.healthai.consult.domain.ConsultationStatus.AI_REVIEWED)
            .createdAt(now)
            .updatedAt(now)
            .build();
        when(consultationMapper.findById(2L)).thenReturn(Optional.of(consultation));
        when(healthProfileMapper.findByUserId(201L)).thenReturn(Optional.empty());

        doAnswer(invocation -> {
            Prescription prescription = invocation.getArgument(0);
            prescription.setId(20L);
            return 1;
        }).when(prescriptionMapper).insert(any(Prescription.class));

        Medicine medicine = medicine();
        when(medicineMapper.findById(11L)).thenReturn(Optional.of(medicine));
        doAnswer(invocation -> {
            PrescriptionItem item = invocation.getArgument(0);
            item.setId(201L);
            return 1;
        }).when(prescriptionItemMapper).insert(any(PrescriptionItem.class));

        when(prescriptionItemMapper.findByPrescriptionId(20L)).thenReturn(List.of(buildItem(201L)));
        ContraindicationReport report = ContraindicationReport.fromViolations(List.of(
            new ContraindicationViolation(0, 11L, "TestMed", "ALLERGY", PrescriptionContraStatus.FAIL, "Allergy conflict")
        ));
        when(contraEvaluator.evaluate(any(), any(), any(), any())).thenReturn(report);

        assertThatThrownBy(() -> prescriptionService.create(new PrescriptionCreateRequest(
            2L,
            301L,
            "note",
            List.of(new PrescriptionItemRequest(11L, "Take one", "daily", 7, BigDecimal.ONE))
        )))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DRUG_CONTRAINDICATED);

        verify(alertingService).notifyContraindicationFailure(20L, report);
    }

    @Test
    void detailShouldReturnAuditsAndItems() {
        Prescription prescription = basePrescription().id(50L).build();
        when(prescriptionMapper.findById(50L)).thenReturn(Optional.of(prescription));
        when(prescriptionItemMapper.findByPrescriptionId(50L)).thenReturn(List.of(buildItem(501L)));
        Medicine medicine = medicine();
        when(medicineMapper.findById(11L)).thenReturn(Optional.of(medicine));
        when(auditMapper.findByPrescriptionId(50L)).thenReturn(List.of(buildAudit(501L)));

        PrescriptionDetailResponse response = prescriptionService.detail(50L);

        assertThat(response.items()).hasSize(1);
        assertThat(response.audits()).hasSize(1);
    }

    @Test
    void searchShouldReturnPageResponse() {
        Prescription prescription = basePrescription().id(80L).build();
        when(prescriptionMapper.search(1L, 2L, 3L, PrescriptionStatus.DRAFT, 20, 0)).thenReturn(List.of(prescription));
        when(prescriptionMapper.count(1L, 2L, 3L, PrescriptionStatus.DRAFT)).thenReturn(1L);

        PageResponse<PrescriptionListResponse.PrescriptionSummary> page = prescriptionService.search(1L, 2L, 3L, "DRAFT", 0, 20);

        assertThat(page.content()).hasSize(1);
        assertThat(page.totalElements()).isEqualTo(1L);
    }

    @Test
    void updateStatusShouldRejectWhenContraFailIssuance() {
        Prescription prescription = basePrescription()
            .id(90L)
            .status(PrescriptionStatus.DRAFT)
            .contraCheckStatus(PrescriptionContraStatus.FAIL)
            .build();
        when(prescriptionMapper.findById(90L)).thenReturn(Optional.of(prescription));

        assertThatThrownBy(() -> prescriptionService.updateStatus(90L, new PrescriptionUpdateStatusRequest("issued", null)))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DRUG_CONTRAINDICATED);
    }

    private Medicine medicine() {
        LocalDateTime now = LocalDateTime.now();
        return Medicine.builder()
            .id(11L)
            .genericName("TestMed")
            .brandName("Brand")
            .indications("flu")
            .contraindications("penicillin")
            .dosageGuideline("max 5 per day")
            .drugInteractions("")
            .tags("")
            .version(1)
            .createdAt(now)
            .updatedAt(now)
            .build();
    }

    private PrescriptionItem buildItem(Long id) {
        LocalDateTime now = LocalDateTime.now();
        return PrescriptionItem.builder()
            .id(id)
            .prescriptionId(10L)
            .medicineId(11L)
            .dosageInstruction("Take one")
            .frequency("daily")
            .daySupply(7)
            .quantity(BigDecimal.ONE)
            .contraResult(PrescriptionContraStatus.PASS)
            .createdAt(now)
            .updatedAt(now)
            .build();
    }

    private ContraindicationAudit buildAudit(Long itemId) {
        LocalDateTime now = LocalDateTime.now();
        return ContraindicationAudit.builder()
            .id(1000L)
            .prescriptionId(50L)
            .prescriptionItemId(itemId)
            .checker("SYSTEM")
            .result(PrescriptionContraStatus.PASS)
            .message("OK")
            .violations("{}")
            .patientSnapshot("{}")
            .checkTime(now)
            .createdAt(now)
            .updatedAt(now)
            .build();
    }

    private Prescription.PrescriptionBuilder basePrescription() {
        LocalDateTime now = LocalDateTime.now();
        return Prescription.builder()
            .consultationId(1L)
            .patientId(200L)
            .doctorId(300L)
            .status(PrescriptionStatus.DRAFT)
            .contraCheckStatus(PrescriptionContraStatus.PASS)
            .createdAt(now)
            .updatedAt(now);
    }
}
