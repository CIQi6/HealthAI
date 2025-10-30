package com.example.healthai.prescription.contra;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.example.healthai.drug.domain.Medicine;
import com.example.healthai.prescription.domain.Prescription;
import com.example.healthai.prescription.domain.PrescriptionContraStatus;
import com.example.healthai.prescription.domain.PrescriptionItem;
import com.example.healthai.profile.domain.HealthProfile;

class SimpleContraindicationEvaluatorTest {

    private final SimpleContraindicationEvaluator evaluator = new SimpleContraindicationEvaluator();

    @Test
    void shouldFailWhenPatientAllergyMatchesMedicineContraindication() {
        Medicine medicine = baseMedicine().contraindications("penicillin, aspirin").build();
        Prescription prescription = basePrescription().build();
        PrescriptionItem item = baseItem().medicineId(medicine.getId()).build();
        HealthProfile profile = baseProfile().allergyHistory("Penicillin").build();

        ContraindicationReport report = evaluator.evaluate(
            prescription,
            List.of(item),
            Map.of(medicine.getId(), medicine),
            profile
        );

        assertThat(report.status()).isEqualTo(PrescriptionContraStatus.FAIL);
        assertThat(report.summary()).contains("过敏史");
        assertThat(report.violations()).hasSize(1);
        assertThat(report.violations().get(0).type()).isEqualTo("ALLERGY");
    }

    @Test
    void shouldWarnWhenPatientDiseaseMatchesMedicineIndication() {
        Medicine medicine = baseMedicine().contraindications("").indications("hypertension, diabetes").build();
        Prescription prescription = basePrescription().build();
        PrescriptionItem item = baseItem().medicineId(medicine.getId()).build();
        HealthProfile profile = baseProfile().chronicDiseases("Hypertension").build();

        ContraindicationReport report = evaluator.evaluate(
            prescription,
            List.of(item),
            Map.of(medicine.getId(), medicine),
            profile
        );

        assertThat(report.status()).isEqualTo(PrescriptionContraStatus.WARN);
        assertThat(report.summary()).contains("慢性病");
        assertThat(report.violations()).hasSize(1);
        assertThat(report.violations().get(0).level()).isEqualTo(PrescriptionContraStatus.WARN);
    }

    @Test
    void shouldFailForHighRiskDrugInteraction() {
        Medicine first = baseMedicine().id(1L).genericName("MedA").drugInteractions("MedB").build();
        Medicine second = baseMedicine().id(2L).genericName("MedB").build();
        Prescription prescription = basePrescription().build();
        PrescriptionItem item1 = baseItem().id(501L).medicineId(1L).build();
        PrescriptionItem item2 = baseItem().id(502L).medicineId(2L).build();

        ContraindicationReport report = evaluator.evaluate(
            prescription,
            List.of(item1, item2),
            Map.of(1L, first, 2L, second),
            baseProfile().build()
        );

        assertThat(report.status()).isEqualTo(PrescriptionContraStatus.FAIL);
        assertThat(report.violations()).hasSize(2);
        assertThat(report.summary()).contains("相互作用");
    }

    @Test
    void shouldFailWhenDosageInstructionMentionsOverdose() {
        Medicine medicine = baseMedicine().dosageGuideline("max 5 per day").build();
        Prescription prescription = basePrescription().build();
        PrescriptionItem item = baseItem().dosageInstruction("Overdose once daily").build();

        ContraindicationReport report = evaluator.evaluate(
            prescription,
            List.of(item),
            Map.of(medicine.getId(), medicine),
            baseProfile().build()
        );

        assertThat(report.status()).isEqualTo(PrescriptionContraStatus.FAIL);
        assertThat(report.summary()).contains("超量");
    }

    @Test
    void shouldFailForPregnancyContraindication() {
        Medicine medicine = baseMedicine().contraindications("pregnancy").build();
        Prescription prescription = basePrescription().build();
        PrescriptionItem item = baseItem().build();
        HealthProfile profile = baseProfile().geneticRisk("pregnant").build();

        ContraindicationReport report = evaluator.evaluate(
            prescription,
            List.of(item),
            Map.of(medicine.getId(), medicine),
            profile
        );

        assertThat(report.status()).isEqualTo(PrescriptionContraStatus.FAIL);
        assertThat(report.summary()).contains("孕期");
    }

    @Test
    void shouldWarnForBloodTypeSpecificContraindication() {
        Medicine medicine = baseMedicine().contraindications("bloodtype:o").build();
        Prescription prescription = basePrescription().build();
        PrescriptionItem item = baseItem().build();
        HealthProfile profile = baseProfile().bloodType("O").build();

        ContraindicationReport report = evaluator.evaluate(
            prescription,
            List.of(item),
            Map.of(medicine.getId(), medicine),
            profile
        );

        assertThat(report.status()).isEqualTo(PrescriptionContraStatus.WARN);
        assertThat(report.summary()).contains("血型");
    }

    private Medicine.MedicineBuilder baseMedicine() {
        LocalDateTime now = LocalDateTime.now();
        return Medicine.builder()
            .id(11L)
            .genericName("TestMed")
            .brandName("TestMed Brand")
            .indications("flu")
            .contraindications("")
            .dosageGuideline("")
            .drugInteractions("")
            .tags("")
            .version(1)
            .createdAt(now)
            .updatedAt(now);
    }

    private Prescription.PrescriptionBuilder basePrescription() {
        LocalDateTime now = LocalDateTime.now();
        return Prescription.builder()
            .id(100L)
            .consultationId(200L)
            .patientId(300L)
            .doctorId(400L)
            .status(com.example.healthai.prescription.domain.PrescriptionStatus.DRAFT)
            .contraCheckStatus(PrescriptionContraStatus.PASS)
            .notes("note")
            .createdAt(now)
            .updatedAt(now);
    }

    private PrescriptionItem.PrescriptionItemBuilder baseItem() {
        LocalDateTime now = LocalDateTime.now();
        return PrescriptionItem.builder()
            .id(500L)
            .prescriptionId(100L)
            .medicineId(11L)
            .dosageInstruction("Take one")
            .frequency("daily")
            .daySupply(7)
            .quantity(BigDecimal.ONE)
            .contraResult(PrescriptionContraStatus.PASS)
            .createdAt(now)
            .updatedAt(now);
    }

    private HealthProfile.HealthProfileBuilder baseProfile() {
        LocalDateTime now = LocalDateTime.now();
        return HealthProfile.builder()
            .id(900L)
            .userId(300L)
            .birthDate(now.toLocalDate())
            .bloodType("O")
            .chronicDiseases("")
            .allergyHistory("")
            .geneticRisk("")
            .createdAt(now)
            .updatedAt(now);
    }
}
