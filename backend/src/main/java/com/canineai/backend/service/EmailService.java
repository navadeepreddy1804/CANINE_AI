package com.canineai.backend.service;

public interface EmailService {

    /**
     * Sends a welcome email to a newly registered clinician.
     * Implementations must be fire-and-forget: never throw, never block
     * the caller, never affect any database transaction.
     *
     * @param toEmail  the recipient's email address
     * @param fullName the recipient's full name for personalisation
     */
    void sendWelcomeEmail(String toEmail, String fullName);

    /**
     * Sends a password-reset token to the given address.
     * Throws on failure — the caller surfaces a friendly error to the user.
     *
     * @param toEmail     the recipient's email address
     * @param resetToken  the one-time reset token
     */
    void sendPasswordResetEmail(String toEmail, String resetToken);

    /**
     * Sends a diagnostic report-ready notification.
     * Best-effort — failures are logged and discarded.
     *
     * @param toEmail      the practitioner's email address
     * @param patientName  the patient's name
     * @param reportId     the generated report identifier
     */
    void sendReportNotificationEmail(String toEmail, String patientName, String reportId);
}
