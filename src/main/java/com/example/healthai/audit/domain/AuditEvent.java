package com.example.healthai.audit.domain;

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
public class AuditEvent extends BaseEntity {

    private Long id;
    private LocalDateTime occurredAt;
    private Long actorId;
    private String actorType;
    private String action;
    private String resourceType;
    private String resourceId;
    private String sourceIp;
    private String metadata;
}
