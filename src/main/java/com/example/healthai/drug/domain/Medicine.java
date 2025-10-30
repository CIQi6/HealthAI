package com.example.healthai.drug.domain;

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
public class Medicine extends BaseEntity {

    private Long id;
    private String genericName;
    private String brandName;
    private String indications;
    private String contraindications;
    private String dosageGuideline;
    private String drugInteractions;
    private String tags;
    private Integer version;
}
