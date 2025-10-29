package com.example.healthai.llm.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.example.healthai.prompt.domain.PromptChannel;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "healthai.llm")
public class LlmProperties {

    private PromptChannel primaryChannel = PromptChannel.OLLAMA;
    private OllamaProperties ollama = new OllamaProperties();
    private HttpApiProperties http = new HttpApiProperties();

    @Getter
    @Setter
    public static class OllamaProperties {

        private boolean enabled = true;
        private String baseUrl = "http://localhost:11434";
        private String model = "llama3";
        private Duration timeout = Duration.ofSeconds(60);
        private Integer maxRetries = 1;
    }

    @Getter
    @Setter
    public static class HttpApiProperties {

        private boolean enabled = false;
        private String baseUrl;
        private String apiKey;
        private String completionPath = "/v1/completions";
        private String model;
        private Duration timeout = Duration.ofSeconds(60);
        private Integer maxRetries = 1;
    }
}
