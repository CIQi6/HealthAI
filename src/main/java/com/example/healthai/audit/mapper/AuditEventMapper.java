package com.example.healthai.audit.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.example.healthai.audit.domain.AuditEvent;

@Mapper
public interface AuditEventMapper {

    int insert(AuditEvent event);
}
