package com.example.healthai.prescription.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import com.example.healthai.AbstractIntegrationTest;
import com.example.healthai.auth.domain.User;
import com.example.healthai.auth.domain.UserType;
import com.example.healthai.consult.domain.Consultation;
import com.example.healthai.consult.domain.ConsultationStatus;
import com.example.healthai.consult.mapper.ConsultationMapper;
import com.example.healthai.drug.domain.Medicine;
import com.example.healthai.drug.mapper.MedicineMapper;
import com.example.healthai.prescription.domain.PrescriptionContraStatus;
import com.example.healthai.prescription.mapper.ContraindicationAuditMapper;
import com.example.healthai.profile.domain.HealthProfile;
import com.example.healthai.profile.mapper.HealthProfileMapper;
import com.fasterxml.jackson.databind.JsonNode;

class PrescriptionControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ConsultationMapper consultationMapper;

    @Autowired
    private MedicineMapper medicineMapper;

    @Autowired
    private HealthProfileMapper healthProfileMapper;

    @Autowired
    private ContraindicationAuditMapper auditMapper;

    @Test
    void shouldCreatePrescriptionAndPassContraCheck() throws Exception {
        User patient = createUser("rx-patient");
        User doctor = createUser("rx-doctor", "Password123", UserType.DOCTOR);
        HealthProfile profile = HealthProfile.builder()
            .userId(patient.getId())
            .birthDate(LocalDate.of(1990, 1, 1))
            .bloodType("O")
            .allergyHistory("lactose")
            .chronicDiseases("hypertension")
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        healthProfileMapper.insert(profile);

        Consultation consultation = Consultation.builder()
            .userId(patient.getId())
            .doctorId(doctor.getId())
            .symptomDescription("头痛")
            .status(ConsultationStatus.DOCTOR_REVIEWED)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        consultationMapper.insert(consultation);

        Medicine medicine = Medicine.builder()
            .genericName("Paracetamol")
            .brandName("Panadol")
            .indications("pain")
            .contraindications("aspirin")
            .dosageGuideline("max 5 per day")
            .drugInteractions("")
            .tags("")
            .version(1)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        medicineMapper.insert(medicine);

        String doctorToken = loginAndGetToken("rx-doctor", "Password123");

        String payload = objectMapper.writeValueAsString(new PrescriptionRequestBuilder()
            .consultationId(consultation.getId())
            .doctorId(doctor.getId())
            .addItem(medicine.getId(), "Take one tablet", "daily", 5, BigDecimal.valueOf(5))
            .build());

        JsonNode response = objectMapper.readTree(mockMvc.perform(post("/api/v1/prescriptions")
                .header("Authorization", "Bearer " + doctorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString());

        JsonNode data = response.path("data");
        assertThat(data.path("contraStatus").asText()).isEqualTo(PrescriptionContraStatus.PASS.name());
        assertThat(data.path("items")).hasSize(1);

        long auditCount = auditMapper.findByPrescriptionId(data.path("id").asLong()).size();
        assertThat(auditCount).isZero();
    }

    @Test
    void shouldBlockPrescriptionWhenContraFails() throws Exception {
        User patient = createUser("rx-patient2");
        User doctor = createUser("rx-doctor2", "Password123", UserType.DOCTOR);
        HealthProfile profile = HealthProfile.builder()
            .userId(patient.getId())
            .allergyHistory("penicillin")
            .birthDate(LocalDate.of(1985, 5, 5))
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        healthProfileMapper.insert(profile);

        Consultation consultation = Consultation.builder()
            .userId(patient.getId())
            .doctorId(doctor.getId())
            .symptomDescription("感染")
            .status(ConsultationStatus.DOCTOR_REVIEWED)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        consultationMapper.insert(consultation);

        Medicine medicine = Medicine.builder()
            .genericName("Penicillin")
            .brandName("PeniBrand")
            .indications("infection")
            .contraindications("penicillin")
            .dosageGuideline("max 3 per day")
            .drugInteractions("")
            .tags("")
            .version(1)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        medicineMapper.insert(medicine);

        String doctorToken = loginAndGetToken("rx-doctor2", "Password123");

        String payload = objectMapper.writeValueAsString(new PrescriptionRequestBuilder()
            .consultationId(consultation.getId())
            .doctorId(doctor.getId())
            .addItem(medicine.getId(), "Take two tablets", "daily", 3, BigDecimal.valueOf(6))
            .build());

        mockMvc.perform(post("/api/v1/prescriptions")
                .header("Authorization", "Bearer " + doctorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isBadRequest());

        assertThat(auditMapper.findByPrescriptionId(1L)).isEmpty();
    }

    @Test
    void shouldReturnDetailAfterCreation() throws Exception {
        shouldCreatePrescriptionAndPassContraCheck();

        JsonNode listResponse = objectMapper.readTree(mockMvc.perform(get("/api/v1/prescriptions")
                .header("Authorization", "Bearer " + loginAndGetToken("rx-doctor", "Password123"))
                .param("doctorId", "2")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString());

        assertThat(listResponse.path("data").path("items")).isNotEmpty();

        long prescriptionId = listResponse.path("data").path("items").get(0).path("id").asLong();
        JsonNode detailResponse = objectMapper.readTree(mockMvc.perform(get("/api/v1/prescriptions/" + prescriptionId)
                .header("Authorization", "Bearer " + loginAndGetToken("rx-doctor", "Password123")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString());

        assertThat(detailResponse.path("data").path("items")).hasSize(1);
    }

    private static class PrescriptionRequestBuilder {

        private final StringBuilder builder = new StringBuilder();
        private boolean firstItem = true;

        PrescriptionRequestBuilder consultationId(Long id) {
            builder.append("{\"consultationId\":").append(id).append(",\"doctorId\":");
            return this;
        }

        PrescriptionRequestBuilder doctorId(Long id) {
            builder.append(id).append(",\"items\":[");
            return this;
        }

        PrescriptionRequestBuilder addItem(Long medicineId, String dosageInstruction, String frequency, Integer daySupply, BigDecimal quantity) {
            if (!firstItem) {
                builder.append(",");
            }
            builder.append("{\"drugId\":").append(medicineId)
                .append(",\"dosageInstruction\":\"").append(dosageInstruction).append("\"")
                .append(",\"frequency\":\"").append(frequency).append("\"")
                .append(",\"daySupply\":").append(daySupply)
                .append(",\"quantity\":").append(quantity)
                .append("}");
            firstItem = false;
            return this;
        }

        String build() {
            builder.append("]}");
            return builder.toString();
        }
    }
}
