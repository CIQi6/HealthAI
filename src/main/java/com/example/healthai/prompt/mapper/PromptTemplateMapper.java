package com.example.healthai.prompt.mapper;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.healthai.prompt.domain.PromptTemplate;

@Mapper
public interface PromptTemplateMapper {

    Optional<PromptTemplate> findActiveByCode(@Param("code") String code);

    List<PromptTemplate> findAllActive();

    Optional<PromptTemplate> findById(@Param("id") Long id);

    List<PromptTemplate> findAll();

    int insert(PromptTemplate template);

    int update(PromptTemplate template);
}
