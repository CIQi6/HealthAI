package com.example.healthai.consult.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.healthai.consult.domain.ConsultationMessage;

@Mapper
public interface ConsultationMessageMapper {

    List<ConsultationMessage> findByConsultationId(@Param("consultationId") Long consultationId);

    int insert(ConsultationMessage message);

    int batchInsert(@Param("messages") List<ConsultationMessage> messages);
}
