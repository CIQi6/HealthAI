package com.example.healthai.prescription.mapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.healthai.prescription.domain.Prescription;
import com.example.healthai.prescription.domain.PrescriptionContraStatus;
import com.example.healthai.prescription.domain.PrescriptionStatus;

@Mapper
public interface PrescriptionMapper {

    Optional<Prescription> findById(@Param("id") Long id);

    List<Prescription> search(@Param("consultationId") Long consultationId,
                               @Param("patientId") Long patientId,
                               @Param("doctorId") Long doctorId,
                               @Param("status") PrescriptionStatus status,
                               @Param("limit") Integer limit,
                               @Param("offset") Integer offset);

    long count(@Param("consultationId") Long consultationId,
               @Param("patientId") Long patientId,
               @Param("doctorId") Long doctorId,
               @Param("status") PrescriptionStatus status);

    int insert(Prescription prescription);

    int update(Prescription prescription);

    int updateStatus(@Param("id") Long id,
                     @Param("status") PrescriptionStatus status,
                     @Param("contraStatus") PrescriptionContraStatus contraStatus,
                     @Param("contraFailReason") String contraFailReason,
                     @Param("updatedAt") LocalDateTime updatedAt);
}
