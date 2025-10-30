package com.example.healthai.prescription.mapper;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.healthai.prescription.domain.PrescriptionItem;

@Mapper
public interface PrescriptionItemMapper {

    Optional<PrescriptionItem> findById(@Param("id") Long id);

    List<PrescriptionItem> findByPrescriptionId(@Param("prescriptionId") Long prescriptionId);

    int insert(PrescriptionItem item);

    int update(PrescriptionItem item);

    int deleteByPrescriptionId(@Param("prescriptionId") Long prescriptionId);
}
