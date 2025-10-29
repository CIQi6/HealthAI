package com.example.healthai.llm.service;

import java.util.EnumMap;
import java.util.Map;

import org.springframework.util.Assert;

import com.example.healthai.llm.client.LlmClient;
import com.example.healthai.prompt.domain.PromptChannel;

public class LlmClientRegistry {

    private final Map<PromptChannel, LlmClient> clients = new EnumMap<>(PromptChannel.class);

    public LlmClientRegistry register(LlmClient client) {
        Assert.notNull(client, "client must not be null");
        clients.put(client.channel(), client);
        return this;
    }

    public LlmClient getClient(PromptChannel channel) {
        return clients.get(channel);
    }

    public LlmClient getDefaultClient() {
        return clients.values().stream().findFirst().orElse(null);
    }
}
