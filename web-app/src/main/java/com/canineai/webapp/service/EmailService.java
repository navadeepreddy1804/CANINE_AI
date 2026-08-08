package com.canineai.webapp.service;

public interface EmailService {
    void sendWelcomeEmail(String toEmail, String doctorName);
    void sendActivityNotification(String toEmail, String action, String description);
}
