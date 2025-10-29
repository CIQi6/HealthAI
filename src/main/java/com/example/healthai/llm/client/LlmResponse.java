package com.example.healthai.llm.client;

import java.time.Duration;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LlmResponse {

    private final String content;
    private final Integer promptTokens;
    private final Integer completionTokens;
    private final Duration latency;
    private final String model;
}
