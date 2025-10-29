package com.example.healthai.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.example.healthai.audit.service.AuditTrailService;
import com.example.healthai.prompt.service.PromptExecutionCommand;
import com.example.healthai.prompt.service.PromptResult;
import com.example.healthai.prompt.service.PromptService;

@TestConfiguration
public class TestRedisConfiguration {

    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        return mock(RedisConnectionFactory.class);
    }

    @Bean
    @Primary
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        var store = new ConcurrentHashMap<String, StoredValue>();

        when(template.opsForValue()).thenReturn(valueOperations);

        when(valueOperations.get(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            StoredValue stored = store.get(key);
            if (stored == null || stored.isExpired()) {
                store.remove(key);
                return null;
            }
            return stored.value();
        });

        doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            String value = invocation.getArgument(1);
            store.put(key, StoredValue.of(value, null));
            return null;
        }).when(valueOperations).set(anyString(), anyString());

        doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            String value = invocation.getArgument(1);
            Duration ttl = invocation.getArgument(2);
            store.put(key, StoredValue.of(value, ttl));
            return null;
        }).when(valueOperations).set(anyString(), anyString(), any(Duration.class));

        doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            String value = invocation.getArgument(1);
            long timeout = invocation.getArgument(2);
            java.util.concurrent.TimeUnit unit = invocation.getArgument(3);
            store.put(key, StoredValue.of(value, Duration.ofMillis(unit.toMillis(timeout))));
            return null;
        }).when(valueOperations).set(anyString(), anyString(), any(Long.class), any(java.util.concurrent.TimeUnit.class));

        when(template.delete(anyString())).thenAnswer(invocation -> store.remove(invocation.getArgument(0)) != null);

        return template;
    }

    @Bean
    @Primary
    public PromptService promptService() {
        PromptService promptService = mock(PromptService.class);
        when(promptService.executeConsultationPrompt(any(PromptExecutionCommand.class)))
            .thenAnswer(invocation -> PromptResult.builder()
                .content("AI diagnosis (test)")
                .model("test-model")
                .build());
        return promptService;
    }

    @Bean
    @Primary
    public AuditTrailService auditTrailService() {
        return mock(AuditTrailService.class);
    }

    private record StoredValue(String value, long expiresAtMillis) {
        static StoredValue of(String value, Duration ttl) {
            long expiresAt = ttl == null ? Long.MAX_VALUE : System.currentTimeMillis() + ttl.toMillis();
            return new StoredValue(value, expiresAt);
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiresAtMillis;
        }
    }
}
