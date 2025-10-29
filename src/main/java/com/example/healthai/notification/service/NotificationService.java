package com.example.healthai.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.healthai.consult.event.ConsultationEventPayload;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final AlertingService alertingService;

    public NotificationService(AlertingService alertingService) {
        this.alertingService = alertingService;
    }

    public String healthProbe() {
        return "notification-service-ok";
    }

    public void handleConsultationEvent(ConsultationEventPayload payload) {
        log.info("[Notification] consultation event received id={} status={} eventType={} ",
            payload.consultationId(), payload.status(), payload.eventType());
        if ("FAILED".equalsIgnoreCase(payload.status())) {
            alertingService.notifyConsultationFailure(payload);
        }
    }
}
