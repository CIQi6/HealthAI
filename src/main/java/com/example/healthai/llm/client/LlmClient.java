package com.example.healthai.llm.client;

import com.example.healthai.prompt.domain.PromptChannel;

public interface LlmClient {

    PromptChannel channel();

    LlmResponse generate(LlmRequest request);
}
