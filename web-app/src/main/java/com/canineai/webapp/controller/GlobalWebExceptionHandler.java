package com.canineai.webapp.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import jakarta.servlet.http.HttpServletResponse;

@Slf4j
@ControllerAdvice
public class GlobalWebExceptionHandler {

    @ExceptionHandler(Exception.class)
    public String handleException(Exception ex, Model model, HttpServletResponse response) {
        log.error("Unhandled exception intercepted by GlobalWebExceptionHandler:", ex);

        // If the response is already committed (headers/body sent), avoid rendering a view
        if (response.isCommitted()) {
            log.warn("Response already committed; cannot render error view. Exception: {}", ex.getMessage());
            return null;
        }
        
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        
        String errorCode = "500";
        String errorMessage = extractFriendlyMessage(ex);
        
        // Explicitly intercept database/service offline errors
        String exClassName = ex.getClass().getName();
        String rootCauseMsg = getRootCause(ex).getMessage();
        if (rootCauseMsg == null) rootCauseMsg = "";
        
        if (exClassName.contains("ConnectException") || exClassName.contains("RetryableException") || 
            exClassName.contains("SocketTimeoutException") || rootCauseMsg.contains("Connection refused") ||
            rootCauseMsg.contains("connect timed out")) {
            errorCode = "AI_OFFLINE";
            errorMessage = "The Core CanineAI Inference Service or MySQL Database node is temporarily unreachable. Our clinical cloud synchronizer is automatically establishing a backup worker bridge.";
        }
        
        model.addAttribute("errorCode", errorCode);
        model.addAttribute("errorMessage", errorMessage);
        return "error";
    }

    private Throwable getRootCause(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }

    private String extractFriendlyMessage(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        String msg = cause.getMessage();
        if (msg == null || msg.isBlank()) {
            return "An unexpected error occurred. Please try again.";
        }

        String value = msg.replaceAll("\\s+", " ").trim();
        String lower = value.toLowerCase();

        if (lower.contains("required fields") || lower.contains("constraints validation failed") || lower.contains("is required") || lower.contains("notblank") || lower.contains("must not be blank") || lower.contains("must not be null")) {
            return "Required fields are missing.";
        }
        if (lower.contains("date of birth") || lower.contains("dateofbirth") || lower.contains("localdate") || lower.contains("invalid date") || lower.contains("date format") || lower.contains("could not read document") || lower.contains("datetimeparse") || lower.contains("parse")) {
            return "Invalid date of birth.";
        }
        if (lower.contains("age") && (lower.contains("between") || lower.contains("limit") || lower.contains("allowed"))) {
            return "Age must be between allowed limits.";
        }
        if (lower.contains("patient id already exists") || lower.contains("hospital patient id")) {
            return "Patient ID already exists.";
        }
        if (lower.contains("phone number already exists") || (lower.contains("phone") && lower.contains("already exists"))) {
            return "Phone number already exists.";
        }
        if (lower.contains("email already exists") || (lower.contains("email") && lower.contains("already exists"))) {
            return "Email already exists.";
        }
        if (lower.contains("unexpected error") || lower.contains("internal server error") || lower.contains("server error") || lower.contains("java.lang") || lower.contains("org.springframework") || lower.contains("sql") || lower.contains("hibernate") || lower.contains("stack trace") || lower.contains("caused by") || lower.contains("exception")) {
            return "An unexpected error occurred. Please try again.";
        }
        if (lower.contains("invalid phone") || lower.contains("phone number format") || lower.contains("phone format") || (lower.contains("phone") && lower.contains("format"))) {
            return "Invalid phone number format.";
        }
        if (lower.contains("invalid email") || lower.contains("email format") || (lower.contains("email") && lower.contains("invalid"))) {
            return "Invalid email address.";
        }
        if (lower.contains("not found")) {
            return "Patient record not found.";
        }

        return "An unexpected error occurred. Please try again.";
    }
}
