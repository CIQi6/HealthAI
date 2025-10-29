package com.example.healthai.audit.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.healthai.audit.domain.AuditEvent;

@Mapper
public interface AuditEventMapper {

    int insert(AuditEvent event);

    List<AuditEvent> search(@Param("resourceType") String resourceType,
                             @Param("action") String action,
                             @Param("actorId") Long actorId,
                             @Param("from") LocalDateTime from,
                             @Param("to") LocalDateTime to,
                             @Param("limit") int limit,
                             @Param("offset") int offset);

    long count(@Param("resourceType") String resourceType,
               @Param("action") String action,
               @Param("actorId") Long actorId,
               @Param("from") LocalDateTime from,
               @Param("to") LocalDateTime to);
}
