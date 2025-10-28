package com.example.healthai.notification.service;

import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    public String healthProbe() {
        return "notification-service-ok";
    }
}
