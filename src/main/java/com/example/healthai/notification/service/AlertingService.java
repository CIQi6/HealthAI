package com.example.healthai.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.healthai.consult.event.ConsultationEventPayload;
import com.example.healthai.prescription.contra.ContraindicationReport;

@Service
public class AlertingService {

    private static final Logger log = LoggerFactory.getLogger(AlertingService.class);

    public void notifyConsultationFailure(ConsultationEventPayload payload) {
        log.error("[Alert] Consultation {} failed. status={} errorCode={} aiModel={}", payload.consultationId(),
            payload.status(), payload.aiErrorCode(), payload.aiModel());
        // TODO integrate with external alerting channels (email, SMS, Ops platform)
    }

    public void notifyContraindicationFailure(Long prescriptionId, ContraindicationReport report) {
        log.error("[Alert] Prescription {} contraindication failure. summary={} violations={}",
            prescriptionId, report.summary(), report.violations());
        // TODO integrate with external alerting channels (email, SMS, Ops platform)
    }
}
