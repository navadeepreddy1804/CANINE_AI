package com.canineai.webapp.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:no-reply@canineai.com}")
    private String fromEmail;

    public EmailServiceImpl(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    private String buildHtmlTemplate(String title, String contentHtml) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "  <style>" +
                "    body { font-family: 'Inter', -apple-system, sans-serif; margin: 0; padding: 0; background-color: #F8FAFC; color: #1E293B; }" +
                "    .container { max-width: 600px; margin: 40px auto; background: #FFFFFF; border: 1px solid #E2E8F0; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05); }" +
                "    .header { background: #0B6EFD; padding: 24px; text-align: center; color: #FFFFFF; }" +
                "    .header h1 { margin: 0; font-size: 24px; font-weight: 800; letter-spacing: -0.5px; }" +
                "    .content { padding: 32px; line-height: 1.6; font-size: 15px; }" +
                "    .footer { background: #F1F5F9; padding: 20px; text-align: center; font-size: 12px; color: #64748B; border-top: 1px solid #E2E8F0; }" +
                "  </style>" +
                "</head>" +
                "<body>" +
                "  <div class='container'>" +
                "    <div class='header'>" +
                "      <h1>CanineAI Suite</h1>" +
                "    </div>" +
                "    <div class='content'>" +
                "      <h2 style='margin-top: 0; color: #0F172A;'>" + title + "</h2>" +
                "      " + contentHtml + "" +
                "    </div>" +
                "    <div class='footer'>" +
                "      &copy; 2026 CanineAI • Secure HIPAA-Compliant Orthodontic Diagnostics" +
                "    </div>" +
                "  </div>" +
                "</body>" +
                "</html>";
    }

    @Override
    public void sendWelcomeEmail(String toEmail, String doctorName) {
        String subject = "Welcome to CanineAI Suite!";
        String contentHtml = "<p>Dear " + doctorName + ",</p>" +
                "<p>Your CanineAI EMR practitioner profile has been created successfully.</p>" +
                "<p>You now have full access to our cloud-based CBCT segmentation analysis, dynamic clinical report generation, and patient records system.</p>" +
                "<p>Best Regards,<br>CanineAI Team</p>";

        attemptEmailDispatch(toEmail, subject, "Welcome to CanineAI", contentHtml);
    }

    @Override
    public void sendActivityNotification(String toEmail, String action, String description) {
        String subject = "CanineAI Activity Alert: " + action;
        String contentHtml = "<p>Hello,</p>" +
                "<p>We recorded the following activity on your CanineAI clinician account:</p>" +
                "<div style='background: #F8FAFC; border: 1px solid #E2E8F0; padding: 16px; border-radius: 8px; margin: 20px 0;'>" +
                "  <p style='margin: 0 0 8px 0;'><strong>Action:</strong> " + action + "</p>" +
                "  <p style='margin: 0;'><strong>Description:</strong> " + description + "</p>" +
                "</div>" +
                "<p>If this was not performed by you, please check your session logs under settings.</p>" +
                "<p>Best Regards,<br>CanineAI Team</p>";

        attemptEmailDispatch(toEmail, subject, action, contentHtml);
    }

    private void attemptEmailDispatch(String toEmail, String subject, String title, String contentHtml) {
        boolean sentSuccessfully = false;

        if (mailSender != null && !fromEmail.equals("no-reply@canineai.com")) {
            try {
                MimeMessage mimeMessage = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
                helper.setFrom(fromEmail);
                helper.setTo(toEmail);
                helper.setSubject(subject);
                helper.setText(buildHtmlTemplate(title, contentHtml), true);

                mailSender.send(mimeMessage);
                log.info("Real SMTP HTML email successfully dispatched to: {}", toEmail);
                sentSuccessfully = true;
            } catch (Exception e) {
                log.warn("Real SMTP HTML dispatch failed. Falling back to console logging: {}", e.getMessage());
            }
        }

        if (!sentSuccessfully) {
            // Print to console outbox fallback
            System.out.println("--------------------------------------------------");
            System.out.println("[SMTP OUTBOX - FALLBACK] HTML EMAIL SENT TO: " + toEmail);
            System.out.println("FROM: " + fromEmail);
            System.out.println("SUBJECT: " + subject);
            System.out.println("HTML CONTENT:\n" + buildHtmlTemplate(title, contentHtml));
            System.out.println("--------------------------------------------------");
        }
    }
}
