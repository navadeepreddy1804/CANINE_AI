package com.canineai.backend.event;

import com.canineai.backend.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listens for WelcomeEmailEvent and dispatches the welcome email
 * after the registration transaction has fully committed to MySQL.
 *
 * Two guarantees are layered here:
 *
 *   1. @TransactionalEventListener(phase = AFTER_COMMIT)
 *      The listener is only invoked after the @Transactional method in
 *      AuthServiceImpl.register() commits. The new user row is guaranteed
 *      to be durable in MySQL before this method runs. If the transaction
 *      rolls back (e.g. a DB constraint violation), this method is never
 *      called and no email is sent.
 *
 *   2. @Async("taskExecutor")
 *      Runs on the dedicated CanineAI-Async thread pool defined in
 *      AsyncConfig. The HTTP request thread returns the 201 response to
 *      the caller immediately — it never waits for email dispatch.
 *
 * Any exception thrown inside this method is caught by AsyncConfig's
 * AsyncUncaughtExceptionHandler, logged, and silently discarded.
 * Registration is never affected by email failures.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WelcomeEmailListener {

    private final EmailService emailService;

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onWelcomeEmail(WelcomeEmailEvent event) {
        log.info("Transaction committed — dispatching welcome email to {}", event.toEmail());
        try {
            emailService.sendWelcomeEmail(event.toEmail(), event.fullName());
        } catch (Exception e) {
            // Safety net — sendWelcomeEmail already swallows internally,
            // but any unexpected exception is caught here and only logged.
            log.warn("Welcome email dispatch failed for {} — registration already succeeded: {}",
                    event.toEmail(), e.getMessage());
        }
    }
}
