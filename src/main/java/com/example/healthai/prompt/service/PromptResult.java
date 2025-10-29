package com.example.healthai.prompt.service;

import com.example.healthai.prompt.domain.PromptChannel;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PromptResult {

    private final String prompt;
    private final PromptChannel channel;
    private final String model;
    private final Integer promptTokens;
    private final Integer completionTokens;
    private final Long latencyMs;
    private final String content;
}
