package com.example.healthai.prescription.domain;

import java.math.BigDecimal;

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
public class PrescriptionItem extends BaseEntity {

    private Long id;
    private Long prescriptionId;
    private Long medicineId;
    private String dosageInstruction;
    private String frequency;
    private Integer daySupply;
    private BigDecimal quantity;
    private PrescriptionContraStatus contraResult;
}
