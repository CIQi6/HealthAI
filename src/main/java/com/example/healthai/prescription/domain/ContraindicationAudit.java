package com.example.healthai.prescription.domain;

import java.time.LocalDateTime;

import com.example.healthai.common.model.BaseEntity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ContraindicationAudit extends BaseEntity {

    private Long id;
    private Long prescriptionId;
    private Long prescriptionItemId;
    private LocalDateTime checkTime;
    private String checker;
    private String patientSnapshot;
    private String violations;
    private PrescriptionContraStatus result;
    private String message;
}
