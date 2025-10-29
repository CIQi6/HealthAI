package com.example.healthai.llm.client;

import java.util.Map;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LlmRequest {

    private final String model;
    private final String prompt;
    private final Map<String, Object> options;
    private final Integer maxTokens;
    private final Double temperature;
}
