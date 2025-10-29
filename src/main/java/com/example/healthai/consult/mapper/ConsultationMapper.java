package com.example.healthai.consult.mapper;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.healthai.consult.domain.Consultation;
import com.example.healthai.consult.domain.ConsultationStatus;

@Mapper
public interface ConsultationMapper {

    Optional<Consultation> findById(@Param("id") Long id);

    List<Consultation> search(@Param("userId") Long userId,
                              @Param("doctorId") Long doctorId,
                              @Param("status") ConsultationStatus status,
                              @Param("limit") Integer limit,
                              @Param("offset") Integer offset);

    int insert(Consultation consultation);

    int update(Consultation consultation);
}
