package com.canineai.backend.service;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Email service implementation.
 *
 * Guarantees:
 *  - sendWelcomeEmail NEVER throws, NEVER blocks the caller, and NEVER
 *    affects any database transaction. It is always invoked from
 *    WelcomeEmailListener which runs @Async AFTER_COMMIT.
 *  - If SMTP credentials are absent or the JavaMailSender bean was not
 *    created, every method degrades to a structured console log so the
 *    application starts and runs cleanly without any mail configuration.
 *  - sendPasswordResetEmail is synchronous and throws on failure so the
 *    caller (AuthServiceImpl.forgotPassword) can surface a clear error.
 *  - sendReportNotificationEmail is best-effort: failures are only logged.
 */
@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    /**
     * Optional — Spring won't create this bean when mail auto-config is
     * excluded or credentials are absent, and that's perfectly fine.
     */
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    public EmailServiceImpl(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // ── internal helpers ────────────────────────────────────────────────────

    /**
     * True only when a sender bean exists AND both username and password are
     * non-blank. A missing password means SMTP auth will fail, so we skip.
     */
    private boolean isEmailConfigured() {
        return mailSender != null
                && fromEmail != null && !fromEmail.isBlank()
                && mailPassword != null && !mailPassword.isBlank();
    }

    /**
     * Wraps arbitrary HTML content in the shared CanineAI branded shell.
     */
    private String buildHtmlTemplate(String title, String bodyHtml) {
        return "<!DOCTYPE html>"
            + "<html lang='en'>"
            + "<head><meta charset='UTF-8'>"
            + "<style>"
            + "  body{margin:0;padding:0;background:#F8FAFC;font-family:'Inter',-apple-system,BlinkMacSystemFont,sans-serif;color:#1E293B}"
            + "  .wrap{max-width:600px;margin:40px auto;background:#fff;border:1px solid #E2E8F0;border-radius:12px;overflow:hidden;box-shadow:0 4px 6px -1px rgba(0,0,0,.06)}"
            + "  .hdr{background:#0B6EFD;padding:28px 32px;text-align:center}"
            + "  .hdr h1{margin:0;color:#fff;font-size:22px;font-weight:800;letter-spacing:-.3px}"
            + "  .hdr p{margin:6px 0 0;color:rgba(255,255,255,.8);font-size:13px}"
            + "  .body{padding:36px 32px;font-size:15px;line-height:1.7}"
            + "  .body h2{margin-top:0;color:#0F172A;font-size:20px}"
            + "  .divider{height:1px;background:#E2E8F0;margin:24px 0}"
            + "  .btn{display:inline-block;background:#0B6EFD;color:#fff!important;text-decoration:none;"
            + "       padding:13px 28px;border-radius:7px;font-weight:600;font-size:14px;margin-top:8px}"
            + "  .chip{display:inline-block;background:#EFF6FF;color:#1D4ED8;border-radius:20px;"
            + "        padding:4px 14px;font-size:12px;font-weight:600;margin-bottom:20px}"
            + "  .ftr{background:#F1F5F9;padding:20px 32px;text-align:center;font-size:12px;color:#64748B;border-top:1px solid #E2E8F0}"
            + "</style></head>"
            + "<body>"
            + "<div class='wrap'>"
            + "  <div class='hdr'>"
            + "    <h1>CanineAI Suite</h1>"
            + "    <p>HIPAA-Compliant Clinical Diagnostics Platform</p>"
            + "  </div>"
            + "  <div class='body'>"
            + "    <span class='chip'>CanineAI</span>"
            + "    <h2>" + title + "</h2>"
            + "    " + bodyHtml
            + "  </div>"
            + "  <div class='ftr'>&copy; 2026 CanineAI &bull; Orthodontic Diagnostics Suite &bull; All rights reserved</div>"
            + "</div>"
            + "</body></html>";
    }

    /**
     * Sends one HTML email. Single attempt — no retries.
     * Throws on any failure so the caller decides how to handle it.
     */
    private void sendHtmlEmail(String toEmail, String subject,
                                String title, String bodyHtml) throws Exception {
        if (!isEmailConfigured()) {
            // SMTP not configured — print to console so nothing is silently lost
            log.info("╔══ [EMAIL — CONSOLE FALLBACK] ══════════════════════════════");
            log.info("║  To:      {}", toEmail);
            log.info("║  Subject: {}", subject);
            log.info("╚════════════════════════════════════════════════════════════");
            return;
        }

        MimeMessage mime = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mime, "utf-8");
        helper.setFrom(fromEmail, "CanineAI Support Team");
        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(buildHtmlTemplate(title, bodyHtml), /* html= */ true);

        mailSender.send(mime);
        log.info("Email dispatched → {}", toEmail);
    }

    // ── public API ──────────────────────────────────────────────────────────

    /**
     * Welcome email sent after a new clinician registers.
     *
     * Contract:
     *  - NEVER throws under any circumstance.
     *  - NEVER blocks the calling thread beyond this method's execution.
     *  - Any failure is logged at WARN and silently discarded.
     *  - Called exclusively from WelcomeEmailListener which is already
     *    running on the @Async thread pool AFTER the transaction commits.
     */
    @Override
    public void sendWelcomeEmail(String toEmail, String fullName) {
        // Normalise the display name — prepend "Dr." if not already present
        String displayName = (fullName == null || fullName.isBlank()) ? "Doctor"
                : fullName.trim().toLowerCase().startsWith("dr")
                        ? fullName.trim()
                        : "Dr. " + fullName.trim();

        String bodyHtml =
            "<p>Dear <strong>" + displayName + "</strong>,</p>"
            + "<p>Your CanineAI clinician account has been created and is ready to use.</p>"
            + "<div class='divider'></div>"
            + "<p><strong>What you can do now:</strong></p>"
            + "<ul style='padding-left:20px;line-height:2'>"
            + "  <li>Upload and analyse CBCT / DICOM scans</li>"
            + "  <li>Generate AI-assisted orthodontic diagnostic reports</li>"
            + "  <li>Manage patient EMR records</li>"
            + "  <li>Export clinical PDF reports</li>"
            + "</ul>"
            + "<div class='divider'></div>"
            + "<p style='color:#64748B;font-size:13px'>"
            + "If you did not create this account, please contact support immediately."
            + "</p>"
            + "<p>Best regards,<br><strong>The CanineAI Team</strong></p>";

        try {
            sendHtmlEmail(toEmail, "Welcome to CanineAI — Your Account Is Ready",
                    "Welcome to CanineAI", bodyHtml);
        } catch (Exception e) {
            // Intentionally swallowed — welcome email is best-effort only.
            // Registration has already succeeded and the user row is in MySQL.
            log.warn("Welcome email failed for {} [non-critical — account already created]: {}",
                    toEmail, e.getMessage());
        }
    }

    /**
     * Password-reset email — synchronous.
     * Throws a RuntimeException on failure so the caller can tell the user
     * that the reset email could not be sent.
     */
    @Override
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        String bodyHtml =
            "<p>Hello,</p>"
            + "<p>A password recovery request was initiated for your CanineAI account.</p>"
            + "<p>Use the token below inside the mobile client to complete the reset:</p>"
            + "<div style='background:#F1F5F9;padding:18px;border-radius:8px;font-family:monospace;"
            + "font-size:20px;font-weight:700;text-align:center;margin:24px 0;"
            + "color:#0F172A;letter-spacing:3px;border:1px solid #E2E8F0'>"
            + resetToken
            + "</div>"
            + "<p style='color:#64748B;font-size:13px'>"
            + "This token expires in 1 hour. If you did not request a password reset, "
            + "you can safely ignore this message."
            + "</p>"
            + "<p>Best regards,<br><strong>The CanineAI Team</strong></p>";

        try {
            sendHtmlEmail(toEmail, "CanineAI — Password Reset Request",
                    "Password Reset", bodyHtml);
        } catch (Exception e) {
            log.error("Password reset email failed for {}: {}", toEmail, e.getMessage());
            throw new RuntimeException(
                    "Unable to send password reset email. Please try again later.", e);
        }
    }

    /**
     * Report-ready notification — synchronous, best-effort.
     * Failures are logged and discarded; report generation is unaffected.
     */
    @Override
    public void sendReportNotificationEmail(String toEmail,
                                             String patientName,
                                             String reportId) {
        String bodyHtml =
            "<p>Dear Practitioner,</p>"
            + "<p>The AI-assisted diagnostic report for patient "
            + "<strong>" + patientName + "</strong> "
            + "(Report ID: <code>" + reportId + "</code>) "
            + "has been generated and is ready to view.</p>"
            + "<p>Open the patient's EMR record in CanineAI to review and export the report.</p>"
            + "<p>Best regards,<br><strong>The CanineAI Team</strong></p>";

        try {
            sendHtmlEmail(toEmail, "CanineAI — Diagnostic Report Ready",
                    "Diagnostic Report Ready", bodyHtml);
        } catch (Exception e) {
            log.warn("Report notification email failed for {} (report {} unaffected): {}",
                    toEmail, reportId, e.getMessage());
        }
    }
}
