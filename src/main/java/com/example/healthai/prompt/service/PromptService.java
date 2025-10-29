package com.example.healthai.prompt.service;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.example.healthai.common.exception.BusinessException;
import com.example.healthai.common.exception.ErrorCode;
import com.example.healthai.llm.client.LlmClient;
import com.example.healthai.llm.client.LlmRequest;
import com.example.healthai.llm.client.LlmResponse;
import com.example.healthai.llm.config.LlmProperties;
import com.example.healthai.llm.service.LlmClientRegistry;
import com.example.healthai.prompt.domain.PromptChannel;
import com.example.healthai.prompt.domain.PromptTemplate;
import com.example.healthai.prompt.mapper.PromptTemplateMapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class PromptService {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{(.*?)}}", Pattern.DOTALL);
    private static final String DEFAULT_CONSULTATION_TEMPLATE_CODE = "consult.initial";

    private final PromptTemplateMapper templateMapper;
    private final LlmClientRegistry clientRegistry;
    private final LlmProperties properties;
    private final ObjectMapper objectMapper;

    public PromptService(PromptTemplateMapper templateMapper,
                         LlmClientRegistry clientRegistry,
                         LlmProperties properties,
                         ObjectMapper objectMapper) {
        this.templateMapper = templateMapper;
        this.clientRegistry = clientRegistry;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public PromptResult executeConsultationPrompt(PromptExecutionCommand command) {
        String templateCode = StringUtils.hasText(command.getTemplateCode())
            ? command.getTemplateCode()
            : DEFAULT_CONSULTATION_TEMPLATE_CODE;

        PromptTemplate template = templateMapper.findActiveByCode(templateCode)
            .orElseThrow(() -> new BusinessException(ErrorCode.PROMPT_TEMPLATE_NOT_FOUND, "未找到提示词模板: " + templateCode));

        Map<String, Object> variables = mergeVariables(template, command.getVariables());
        String renderedPrompt = renderTemplate(template.getContent(), variables);

        PromptChannel channel = command.getChannelOverride() != null ? command.getChannelOverride() : template.getChannel();
        LlmClient client = resolveClient(channel);

        String model = determineModel(template, command.getModelOverride(), channel);

        LlmResponse response = client.generate(LlmRequest.builder()
            .model(model)
            .prompt(renderedPrompt)
            .options(command.getOptions())
            .maxTokens(command.getMaxTokens())
            .temperature(command.getTemperature())
            .build());

        return PromptResult.builder()
            .prompt(renderedPrompt)
            .channel(channel)
            .model(response.getModel())
            .promptTokens(response.getPromptTokens())
            .completionTokens(response.getCompletionTokens())
            .latencyMs(extractLatency(response.getLatency()))
            .content(response.getContent())
            .build();
    }

    private Map<String, Object> mergeVariables(PromptTemplate template, Map<String, Object> commandVariables) {
        Map<String, Object> merged = new HashMap<>();
        if (StringUtils.hasText(template.getVariables())) {
            try {
                Map<String, Object> templateVars = objectMapper.readValue(template.getVariables(), new TypeReference<>() {
                });
                merged.putAll(templateVars);
            } catch (IOException e) {
                throw new BusinessException(ErrorCode.PROMPT_RENDER_FAILED, "模板变量解析失败", e);
            }
        }
        if (commandVariables != null) {
            merged.putAll(commandVariables);
        }
        return merged;
    }

    private String renderTemplate(String content, Map<String, Object> variables) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(content);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1).trim();
            Object replacement = variables.getOrDefault(key, "");
            String replacementStr = replacement == null ? "" : replacement.toString();
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacementStr));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private LlmClient resolveClient(PromptChannel channel) {
        PromptChannel resolvedChannel = channel != null ? channel : properties.getPrimaryChannel();
        LlmClient client = Optional.ofNullable(clientRegistry.getClient(resolvedChannel))
            .orElseGet(() -> clientRegistry.getClient(properties.getPrimaryChannel()));
        if (client == null) {
            throw new BusinessException(ErrorCode.LLM_CALL_FAILED, "未找到可用的大模型客户端");
        }
        return client;
    }

    private String determineModel(PromptTemplate template, String override, PromptChannel channel) {
        if (StringUtils.hasText(override)) {
            return override;
        }
        if (StringUtils.hasText(template.getModelName())) {
            return template.getModelName();
        }
        return switch (channel != null ? channel : properties.getPrimaryChannel()) {
            case OLLAMA -> properties.getOllama().getModel();
            case HTTP_API -> properties.getHttp().getModel();
        };
    }

    private Long extractLatency(Duration latency) {
        return latency == null ? null : latency.toMillis();
    }
}
