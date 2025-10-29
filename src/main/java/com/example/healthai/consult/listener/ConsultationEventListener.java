package com.example.healthai.consult.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.example.healthai.consult.event.ConsultationEventPayload;
import com.example.healthai.notification.service.NotificationService;

@Component
@ConditionalOnProperty(prefix = "healthai.kafka.consultation", name = "consumer-enabled", havingValue = "true")
public class ConsultationEventListener {

    private static final Logger log = LoggerFactory.getLogger(ConsultationEventListener.class);

    private final NotificationService notificationService;

    public ConsultationEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = {
        "${healthai.kafka.consultation.created-topic}",
        "${healthai.kafka.consultation.ai-reviewed-topic}",
        "${healthai.kafka.consultation.reviewed-topic}",
        "${healthai.kafka.consultation.closed-topic}",
        "${healthai.kafka.consultation.failed-topic}"
    })
    public void onConsultationEvent(@Payload ConsultationEventPayload payload) {
        log.debug("Kafka consultation event consumed id={} status={} type={} ",
            payload.consultationId(), payload.status(), payload.eventType());
        notificationService.handleConsultationEvent(payload);
    }
}
