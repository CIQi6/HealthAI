package com.example.healthai.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import com.example.healthai.llm.client.LlmClient;
import com.example.healthai.llm.client.http.HttpApiLlmClient;
import com.example.healthai.llm.client.ollama.OllamaLlmClient;
import com.example.healthai.llm.config.LlmProperties;
import com.example.healthai.llm.service.LlmClientRegistry;

@Configuration
@EnableConfigurationProperties(LlmProperties.class)
public class LlmConfiguration {

    @Bean
    public LlmClientRegistry llmClientRegistry(LlmProperties properties) {
        LlmClientRegistry registry = new LlmClientRegistry();
        if (properties.getOllama().isEnabled()) {
            registry.register(ollamaClient(properties));
        }
        if (properties.getHttp().isEnabled()) {
            registry.register(httpApiClient(properties));
        }
        return registry;
    }

    @Bean
    @ConditionalOnProperty(prefix = "healthai.llm.ollama", name = "enabled", havingValue = "true", matchIfMissing = true)
    public LlmClient ollamaClient(LlmProperties properties) {
        LlmProperties.OllamaProperties config = properties.getOllama();
        RestClient restClient = RestClient.builder()
            .baseUrl(config.getBaseUrl())
            .requestFactory(createRequestFactory(config.getTimeout().toMillis()))
            .build();
        return new OllamaLlmClient(restClient, config);
    }

    @Bean
    @ConditionalOnProperty(prefix = "healthai.llm.http", name = "enabled", havingValue = "true")
    public LlmClient httpApiClient(LlmProperties properties) {
        LlmProperties.HttpApiProperties config = properties.getHttp();
        RestClient.Builder builder = RestClient.builder()
            .baseUrl(config.getBaseUrl())
            .requestFactory(createRequestFactory(config.getTimeout().toMillis()));
        if (StringUtils.hasText(config.getApiKey())) {
            builder = builder.defaultHeader("Authorization", "Bearer " + config.getApiKey());
        }
        RestClient restClient = builder.build();
        return new HttpApiLlmClient(restClient, config);
    }

    private SimpleClientHttpRequestFactory createRequestFactory(long timeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int timeout = (int) timeoutMs;
        factory.setReadTimeout(timeout);
        factory.setConnectTimeout(timeout);
        return factory;
    }
}
