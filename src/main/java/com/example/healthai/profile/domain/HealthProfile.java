package com.example.healthai.profile.domain;

import java.time.LocalDate;

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
public class HealthProfile extends BaseEntity {

    private Long id;
    private Long userId;
    private LocalDate birthDate;
    private String bloodType;
    private String chronicDiseases;
    private String allergyHistory;
    private String geneticRisk;
}
