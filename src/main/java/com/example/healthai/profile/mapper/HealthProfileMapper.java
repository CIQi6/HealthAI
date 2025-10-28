package com.example.healthai.profile.mapper;

import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.healthai.profile.domain.HealthProfile;

@Mapper
public interface HealthProfileMapper {

    Optional<HealthProfile> findByUserId(@Param("userId") Long userId);

    Optional<HealthProfile> findById(@Param("id") Long id);

    int insert(HealthProfile profile);

    int update(HealthProfile profile);

    int delete(@Param("id") Long id);
}
