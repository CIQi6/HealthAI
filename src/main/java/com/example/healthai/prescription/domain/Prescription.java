package com.example.healthai.prescription.domain;

import java.util.List;

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
public class Prescription extends BaseEntity {

    private Long id;
    private Long consultationId;
    private Long patientId;
    private Long doctorId;
    private PrescriptionStatus status;
    private String notes;
    private PrescriptionContraStatus contraCheckStatus;
    private String contraFailReason;
    private List<PrescriptionItem> items;
}
