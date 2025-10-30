package com.example.healthai.prescription.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.healthai.prescription.domain.ContraindicationAudit;

@Mapper
public interface ContraindicationAuditMapper {

    List<ContraindicationAudit> findByPrescriptionId(@Param("prescriptionId") Long prescriptionId);

    int insert(ContraindicationAudit audit);
}
