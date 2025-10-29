package com.example.healthai.consult.domain;

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
public class Consultation extends BaseEntity {

    private Long id;
    private Long userId;
    private Long doctorId;
    private String symptomDescription;
    private String aiDiagnosis;
    private String doctorOpinion;
    private ConsultationStatus status;
    private String aiModel;
    private Integer aiLatencyMs;
    private String aiErrorCode;
    private LocalDateTime closedAt;
}
