package com.example.healthai.consult.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "healthai.kafka.consultation")
public class ConsultationKafkaProperties {

    private String createdTopic = "healthai.consultations.created";
    private String aiReviewedTopic = "healthai.consultations.ai_reviewed";
    private String reviewedTopic = "healthai.consultations.reviewed";
    private String closedTopic = "healthai.consultations.closed";
    private String failedTopic = "healthai.consultations.failed";
}
