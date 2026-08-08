package com.canineai.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * CanineAI Backend entry point.
 *
 * Mail auto-configuration is controlled via application-dev.yml (excluded
 * when credentials are absent). When MAIL_USERNAME + MAIL_PASSWORD are set
 * in the environment, auto-config creates a JavaMailSender bean and welcome
 * emails are dispatched. When they are absent, EmailServiceImpl detects
 * mailSender == null (required=false) and falls back to console logging.
 * Registration ALWAYS succeeds independently of email configuration.
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class BackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}
