package com.example.healthai.consult.domain;

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
public class ConsultationMessage extends BaseEntity {

    private Long id;
    private Long consultationId;
    private ConsultationMessageRole role;
    private Integer sequenceNo;
    private String content;
    private Integer tokenUsage;
}
