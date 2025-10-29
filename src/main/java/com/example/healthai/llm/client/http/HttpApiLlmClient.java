package com.example.healthai.llm.client.http;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.util.CollectionUtils;
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

@RequiredArgsConstructor
public class HttpApiLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(HttpApiLlmClient.class);

    private final RestClient restClient;
    private final LlmProperties.HttpApiProperties properties;

    @Override
    public PromptChannel channel() {
        return PromptChannel.HTTP_API;
    }

    @Override
    public LlmResponse generate(LlmRequest request) {
        if (!StringUtils.hasText(properties.getModel()) && !StringUtils.hasText(request.getModel())) {
            throw new BusinessException(ErrorCode.LLM_CALL_FAILED, "未配置 HTTP API 模型");
        }

        String model = StringUtils.hasText(request.getModel()) ? request.getModel() : properties.getModel();

        HttpCompletionRequest payload = new HttpCompletionRequest(model, request.getPrompt(), request.getMaxTokens(),
            request.getTemperature(), request.getOptions());

        RestClientException lastException = null;
        int attempts = properties.getMaxRetries() != null && properties.getMaxRetries() > 0 ? properties.getMaxRetries() : 1;

        for (int i = 0; i < attempts; i++) {
            Instant start = Instant.now();
            try {
                HttpCompletionResponse response = restClient.post()
                    .uri(properties.getCompletionPath())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(HttpCompletionResponse.class);

                String content = extractContent(response);
                if (!StringUtils.hasText(content)) {
                    throw new BusinessException(ErrorCode.LLM_CALL_FAILED, "HTTP 模型返回为空");
                }

                Duration latency = Duration.between(start, Instant.now());
                Integer promptTokens = response != null && response.getUsage() != null ? response.getUsage().getPromptTokens() : null;
                Integer completionTokens = response != null && response.getUsage() != null ? response.getUsage().getCompletionTokens() : null;

                return LlmResponse.builder()
                    .content(content)
                    .promptTokens(promptTokens)
                    .completionTokens(completionTokens)
                    .latency(latency)
                    .model(model)
                    .build();
            } catch (ResourceAccessException ex) {
                lastException = ex;
                log.warn("[HTTP LLM] 调用超时 (attempt={})", i + 1, ex);
                if (i == attempts - 1) {
                    throw new BusinessException(ErrorCode.LLM_TIMEOUT, "HTTP 模型响应超时", ex);
                }
            } catch (RestClientResponseException ex) {
                log.error("[HTTP LLM] 调用失败: status={} body={}", ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
                throw new BusinessException(ErrorCode.LLM_CALL_FAILED, "HTTP 模型调用失败: " + ex.getStatusText(), ex);
            } catch (RestClientException ex) {
                lastException = ex;
                log.warn("[HTTP LLM] 调用异常 (attempt={})", i + 1, ex);
                if (i == attempts - 1) {
                    throw new BusinessException(ErrorCode.LLM_CALL_FAILED, "HTTP 模型调用异常", ex);
                }
            }
        }

        throw new BusinessException(ErrorCode.LLM_CALL_FAILED, "HTTP 模型调用失败", lastException);
    }

    private String extractContent(HttpCompletionResponse response) {
        if (response == null || CollectionUtils.isEmpty(response.getChoices())) {
            return null;
        }
        HttpCompletionResponse.Choice first = response.getChoices().get(0);
        if (first == null) {
            return null;
        }
        if (StringUtils.hasText(first.getText())) {
            return first.getText();
        }
        return first.getMessage() != null ? first.getMessage().get("content") : null;
    }

    @RequiredArgsConstructor
    private static class HttpCompletionRequest {

        private final String model;
        private final String prompt;
        private final Integer maxTokens;
        private final Double temperature;
        private final Map<String, Object> options;

        public String getModel() {
            return model;
        }

        public String getPrompt() {
            return prompt;
        }

        public Integer getMaxTokens() {
            return maxTokens;
        }

        public Double getTemperature() {
            return temperature;
        }

        public Map<String, Object> getOptions() {
            return options == null ? Collections.emptyMap() : options;
        }
    }

    @Getter
    private static class HttpCompletionResponse {

        private List<Choice> choices;
        private Usage usage;

        @Getter
        private static class Choice {

            private String text;
            private Map<String, String> message;
        }

        @Getter
        private static class Usage {

            private Integer promptTokens;
            private Integer completionTokens;
        }
    }
}
