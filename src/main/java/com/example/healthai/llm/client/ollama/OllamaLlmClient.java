package com.example.healthai.llm.client.ollama;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.example.healthai.common.exception.BusinessException;
import com.example.healthai.common.exception.ErrorCode;
import com.example.healthai.llm.client.LlmClient;
import com.example.healthai.llm.client.LlmRequest;
import com.example.healthai.llm.client.LlmResponse;
import com.example.healthai.llm.config.LlmProperties;
import com.example.healthai.prompt.domain.PromptChannel;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

public class OllamaLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(OllamaLlmClient.class);

    private final RestClient restClient;
    private final LlmProperties.OllamaProperties properties;

    public OllamaLlmClient(RestClient restClient, LlmProperties.OllamaProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @Override
    public PromptChannel channel() {
        return PromptChannel.OLLAMA;
    }
    @Override
    public LlmResponse generate(LlmRequest request) {

        String model = StringUtils.hasText(request.getModel()) ? request.getModel() : properties.getModel();
        if (!StringUtils.hasText(model)) {
            throw new BusinessException(ErrorCode.LLM_CALL_FAILED, "未配置 Ollama 模型");
        }

        OllamaRequest payload = new OllamaRequest(model, request.getPrompt(), request.getOptions(), request.getMaxTokens(), request.getTemperature());
        RestClientException lastException = null;
        int attempts = properties.getMaxRetries() != null && properties.getMaxRetries() > 0 ? properties.getMaxRetries() : 1;

        for (int i = 0; i < attempts; i++) {
            Instant start = Instant.now();
            try {
                OllamaResponse response = restClient.post()
                    .uri("/api/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(OllamaResponse.class);

                if (response == null || !StringUtils.hasText(response.getResponse())) {
                    throw new BusinessException(ErrorCode.LLM_CALL_FAILED, "Ollama 返回结果为空");
                }

                Duration latency = Duration.between(start, Instant.now());
                return LlmResponse.builder()
                    .content(response.getResponse())
                    .promptTokens(response.getPromptEvalCount())
                    .completionTokens(response.getEvalCount())
                    .latency(latency)
                    .model(StringUtils.hasText(response.getModel()) ? response.getModel() : model)
                    .build();
            } catch (ResourceAccessException ex) {
                lastException = ex;
                log.warn("[Ollama] 调用超时 (attempt={})", i + 1, ex);
                if (i == attempts - 1) {
                    throw new BusinessException(ErrorCode.LLM_TIMEOUT, "Ollama 响应超时", ex);
                }
            } catch (RestClientResponseException ex) {
                log.error("[Ollama] 调用失败: status={} body={}", ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
                throw new BusinessException(ErrorCode.LLM_CALL_FAILED, "Ollama 调用失败: " + ex.getStatusText(), ex);
            } catch (RestClientException ex) {
                lastException = ex;
                log.warn("[Ollama] 调用异常 (attempt={})", i + 1, ex);
                if (i == attempts - 1) {
                    throw new BusinessException(ErrorCode.LLM_CALL_FAILED, "Ollama 调用异常", ex);
                }
            }
        }

        throw new BusinessException(ErrorCode.LLM_CALL_FAILED, "Ollama 调用失败", lastException);
    }

    @RequiredArgsConstructor
    private static class OllamaRequest {

        private final String model;
        private final String prompt;
        private final Map<String, Object> options;
        private final Integer maxTokens;
        private final Double temperature;

        public String getModel() {
            return model;
        }

        public String getPrompt() {
            return prompt;
        }

        public Map<String, Object> getOptions() {
            return options;
        }

        public Integer getMaxTokens() {
            return maxTokens;
        }

        public Double getTemperature() {
            return temperature;
        }
    }

    @Getter
    private static class OllamaResponse {

        private String response;
        private String model;
        private Integer promptEvalCount;
        private Integer evalCount;
    }
}
