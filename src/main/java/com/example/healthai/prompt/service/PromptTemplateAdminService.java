package com.example.healthai.prompt.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.healthai.common.exception.BusinessException;
import com.example.healthai.common.exception.ErrorCode;
import com.example.healthai.prompt.domain.PromptTemplate;
import com.example.healthai.prompt.dto.PromptTemplateRequest;
import com.example.healthai.prompt.dto.PromptTemplateResponse;
import com.example.healthai.prompt.mapper.PromptTemplateMapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class PromptTemplateAdminService {

    private static final Logger log = LoggerFactory.getLogger(PromptTemplateAdminService.class);

    private final PromptTemplateMapper promptTemplateMapper;
    private final ObjectMapper objectMapper;

    public PromptTemplateAdminService(PromptTemplateMapper promptTemplateMapper, ObjectMapper objectMapper) {
        this.promptTemplateMapper = promptTemplateMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<PromptTemplateResponse> list(Boolean includeDisabled) {
        boolean loadAll = includeDisabled != null && includeDisabled;
        List<PromptTemplate> templates = loadAll
            ? promptTemplateMapper.findAll()
            : promptTemplateMapper.findAllActive();
        return templates.stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public PromptTemplateResponse detail(Long id) {
        return promptTemplateMapper.findById(id)
            .map(this::toResponse)
            .orElseThrow(() -> new BusinessException(ErrorCode.PROMPT_TEMPLATE_NOT_FOUND, "提示词模板不存在"));
    }

    @Transactional
    public PromptTemplateResponse create(PromptTemplateRequest request) {
        LocalDateTime now = LocalDateTime.now();
        PromptTemplate template = PromptTemplate.builder()
            .code(request.code())
            .version(request.version())
            .channel(request.channel())
            .modelName(request.modelName())
            .language(request.language())
            .description(request.description())
            .content(request.content())
            .variables(serializeVariables(request))
            .enabled(request.enabled() == null || request.enabled())
            .createdAt(now)
            .updatedAt(now)
            .build();
        promptTemplateMapper.insert(template);
        return toResponse(template);
    }

    @Transactional
    public PromptTemplateResponse update(Long id, PromptTemplateRequest request) {
        PromptTemplate existing = promptTemplateMapper.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.PROMPT_TEMPLATE_NOT_FOUND, "提示词模板不存在"));
        existing.setChannel(request.channel());
        existing.setModelName(request.modelName());
        existing.setLanguage(request.language());
        existing.setVersion(request.version());
        existing.setDescription(request.description());
        existing.setContent(request.content());
        existing.setVariables(serializeVariables(request));
        if (request.enabled() != null) {
            existing.setEnabled(request.enabled());
        }
        existing.setUpdatedAt(LocalDateTime.now());
        promptTemplateMapper.update(existing);
        return toResponse(existing);
    }

    @Transactional
    public void delete(Long id) {
        updateStatus(id, false);
    }

    @Transactional
    public PromptTemplateResponse updateStatus(Long id, boolean enabled) {
        PromptTemplate existing = promptTemplateMapper.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.PROMPT_TEMPLATE_NOT_FOUND, "提示词模板不存在"));
        existing.setEnabled(enabled);
        existing.setUpdatedAt(LocalDateTime.now());
        promptTemplateMapper.update(existing);
        return toResponse(existing);
    }

    private PromptTemplateResponse toResponse(PromptTemplate template) {
        return new PromptTemplateResponse(
            template.getId(),
            template.getCode(),
            template.getVersion(),
            template.getChannel(),
            template.getModelName(),
            template.getLanguage(),
            template.getDescription(),
            template.getContent(),
            deserializeVariables(template.getVariables()),
            template.isEnabled(),
            template.getCreatedAt(),
            template.getUpdatedAt()
        );
    }

    private String serializeVariables(PromptTemplateRequest request) {
        if (request.variables() == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(request.variables());
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.PROMPT_RENDER_FAILED, "提示词变量序列化失败", e);
        }
    }

    private Map<String, Object> deserializeVariables(String variablesJson) {
        if (variablesJson == null || variablesJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(variablesJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize prompt template variables: {}", variablesJson, e);
            return Map.of();
        }
    }
}
