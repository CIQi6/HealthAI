package com.example.healthai.consult.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import com.example.healthai.consult.event.ConsultationEventPayload;

@Configuration
@EnableConfigurationProperties(ConsultationKafkaProperties.class)
public class ConsultationKafkaConfiguration {

    @Bean
    @ConditionalOnBean(ProducerFactory.class)
    public KafkaTemplate<String, ConsultationEventPayload> consultationKafkaTemplate(
            ProducerFactory<String, ConsultationEventPayload> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}
