package com.canineai.backend.common;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.List;
import java.util.Locale;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        RequestDiagnostics diag = extractDiagnostics(request);
        log.warn("Business exception [correlationId={}, user={}, endpoint={}, patientId={}, studyId={}, sessionId={}, jobId={}]: Code={}, Message={}",
                diag.correlationId, diag.user, diag.endpoint, diag.patientId, diag.studyId, diag.uploadSessionId, diag.aiJobId,
                ex.getErrorCode().getCode(), ex.getMessage());

        ErrorCode err = ex.getErrorCode();
        String message = sanitizeMessage(ex.getMessage(), "An unexpected error occurred. Please try again.");
        ApiResponse<Void> res = ApiResponse.error(err.getCode(), message, null);
        return new ResponseEntity<>(res, err.getStatus());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        RequestDiagnostics diag = extractDiagnostics(request);
        log.warn("Validation error [correlationId={}, user={}, endpoint={}, errorsCount={}]: {}",
                diag.correlationId, diag.user, diag.endpoint, ex.getBindingResult().getErrorCount(), ex.getMessage());

        String message = resolveValidationMessage(ex.getBindingResult().getFieldErrors());
        ApiResponse<Void> res = ApiResponse.error(
                ErrorCode.INVALID_INPUT.getCode(),
                message,
                null
        );

        return new ResponseEntity<>(res, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        RequestDiagnostics diag = extractDiagnostics(request);
        String cause = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
        log.warn("Malformed request body [correlationId={}, user={}, endpoint={}]: {}",
                diag.correlationId, diag.user, diag.endpoint, cause);
        String message = "Malformed request payload.";
        if (cause != null && (cause.toLowerCase().contains("date") || cause.toLowerCase().contains("localdate") || cause.toLowerCase().contains("time"))) {
            message = "Invalid date of birth.";
        }
        ApiResponse<Void> res = ApiResponse.error(
                ErrorCode.INVALID_INPUT.getCode(),
                message,
                null
        );
        return new ResponseEntity<>(res, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        RequestDiagnostics diag = extractDiagnostics(request);
        log.warn("Access denied [correlationId={}, user={}, endpoint={}]: {}",
                diag.correlationId, diag.user, diag.endpoint, ex.getMessage());
        ApiResponse<Void> res = ApiResponse.error(
                ErrorCode.FORBIDDEN.getCode(),
                "Access denied: insufficient permission privileges",
                null
        );
        return new ResponseEntity<>(res, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(org.springframework.dao.DataIntegrityViolationException ex, HttpServletRequest request) {
        RequestDiagnostics diag = extractDiagnostics(request);
        log.warn("Database integrity violation [correlationId={}, user={}, endpoint={}]: {}",
                diag.correlationId, diag.user, diag.endpoint, ex.getMessage());
        String msg = "A patient record with the same unique value already exists.";
        String errorText = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
        if (errorText.contains("patients.email") || errorText.contains("email")) {
            msg = "Email already exists.";
        } else if (errorText.contains("patients.phone") || errorText.contains("phone")) {
            msg = "Phone number already exists.";
        }
        ApiResponse<Void> res = ApiResponse.error(
                ErrorCode.CONFLICT.getCode(),
                msg,
                null
        );
        return new ResponseEntity<>(res, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAllExceptions(Exception ex, HttpServletRequest request) {
        RequestDiagnostics diag = extractDiagnostics(request);

        log.error("Internal Server Error 500 intercepted [correlationId={}, user={}, endpoint={}, patientId={}, studyId={}, sessionId={}, jobId={}]: {}",
                diag.correlationId, diag.user, diag.endpoint, diag.patientId, diag.studyId, diag.uploadSessionId, diag.aiJobId,
                ex.getMessage(), ex);

        ApiResponse<Void> res = ApiResponse.error(
                ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                "An unexpected error occurred. Please try again.",
                null
        );
        return new ResponseEntity<>(res, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private RequestDiagnostics extractDiagnostics(HttpServletRequest request) {
        String uri = request != null ? request.getRequestURI() : "UNKNOWN";
        String method = request != null ? request.getMethod() : "UNKNOWN";
        String queryString = (request != null && request.getQueryString() != null) ? "?" + request.getQueryString() : "";
        String endpoint = method + " " + uri + queryString;

        String user = "ANONYMOUS";
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            user = auth.getName();
        }

        String correlationId = org.slf4j.MDC.get("correlationId");
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = request != null ? request.getHeader("X-Correlation-ID") : "N/A";
        }

        String patientId = request != null ? request.getParameter("patientId") : null;
        String studyId = request != null ? request.getParameter("studyId") : null;
        String uploadSessionId = request != null ? request.getParameter("sessionId") : null;
        if (uploadSessionId == null && request != null) uploadSessionId = request.getParameter("uploadSessionId");
        String aiJobId = request != null ? request.getParameter("jobId") : null;

        // Extract identifiers from standard REST URI paths if not in query parameters
        if (uri != null) {
            String[] parts = uri.split("/");
            for (int i = 0; i < parts.length - 1; i++) {
                String seg = parts[i].toLowerCase();
                String next = parts[i + 1];
                if ("patients".equals(seg) && patientId == null && !next.isEmpty()) patientId = next;
                if ("studies".equals(seg) && studyId == null && !next.isEmpty()) studyId = next;
                if ("uploads".equals(seg) && uploadSessionId == null && !next.isEmpty()) uploadSessionId = next;
                if ("jobs".equals(seg) && aiJobId == null && !next.isEmpty()) aiJobId = next;
            }
        }

        return new RequestDiagnostics(endpoint, user, correlationId != null ? correlationId : "N/A",
                patientId, studyId, uploadSessionId, aiJobId);
    }

    private static class RequestDiagnostics {
        final String endpoint;
        final String user;
        final String correlationId;
        final String patientId;
        final String studyId;
        final String uploadSessionId;
        final String aiJobId;

        RequestDiagnostics(String endpoint, String user, String correlationId,
                           String patientId, String studyId, String uploadSessionId, String aiJobId) {
            this.endpoint = endpoint;
            this.user = user;
            this.correlationId = correlationId;
            this.patientId = patientId;
            this.studyId = studyId;
            this.uploadSessionId = uploadSessionId;
            this.aiJobId = aiJobId;
        }
    }

    private String resolveValidationMessage(List<FieldError> fieldErrors) {
        if (fieldErrors == null || fieldErrors.isEmpty()) {
            return "Required fields are missing.";
        }

        boolean hasDateField = fieldErrors.stream().anyMatch(error ->
                error.getField().toLowerCase().contains("dob") || error.getField().toLowerCase().contains("dateofbirth") || error.getField().toLowerCase().contains("date_of_birth"));
        if (hasDateField) {
            return "Invalid date of birth.";
        }

        boolean hasAgeField = fieldErrors.stream().anyMatch(error -> error.getField().toLowerCase().contains("age"));
        if (hasAgeField) {
            return "Age must be between allowed limits.";
        }

        boolean hasPhoneField = fieldErrors.stream().anyMatch(error -> error.getField().toLowerCase().contains("phone"));
        if (hasPhoneField) {
            return "Invalid phone number format.";
        }

        boolean hasEmailField = fieldErrors.stream().anyMatch(error -> error.getField().toLowerCase().contains("email"));
        if (hasEmailField) {
            return "Invalid email address.";
        }

        return "Required fields are missing.";
    }

    private String sanitizeMessage(String rawMessage, String defaultMessage) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return defaultMessage;
        }

        String value = rawMessage.replaceAll("\\s+", " ").trim();
        String lower = value.toLowerCase(Locale.ROOT);

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
        if (lower.contains("unexpected error") || lower.contains("internal server error") || lower.contains("server error") || lower.contains("java.lang") || lower.contains("org.springframework") || lower.contains("sql") || lower.contains("hibernate") || lower.contains("stack trace") || lower.contains("caused by")) {
            return "An unexpected error occurred. Please try again.";
        }
        if (lower.contains("invalid phone") || lower.contains("phone number format") || lower.contains("phone format") || (lower.contains("phone") && lower.contains("format"))) {
            return "Invalid phone number format.";
        }
        if (lower.contains("invalid email") || lower.contains("email format") || (lower.contains("email address") && lower.contains("invalid"))) {
            return "Invalid email address.";
        }
        if (lower.contains("patient record not found") || (lower.contains("patient") && lower.contains("not found"))) {
            return "Patient record not found.";
        }

        return value;
    }
}
