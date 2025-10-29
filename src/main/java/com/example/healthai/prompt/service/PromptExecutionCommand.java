package com.example.healthai.prompt.service;

import java.util.Collections;
import java.util.Map;

import com.example.healthai.prompt.domain.PromptChannel;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PromptExecutionCommand {

    private String templateCode;

    @Builder.Default
    private Map<String, Object> variables = Collections.emptyMap();

    private PromptChannel channelOverride;
    private String modelOverride;
    private Integer maxTokens;
    private Double temperature;

    @Builder.Default
    private Map<String, Object> options = Collections.emptyMap();
}
