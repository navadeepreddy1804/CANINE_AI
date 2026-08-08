package com.canineai.backend.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    INTERNAL_SERVER_ERROR("SYS_500", "An unexpected internal server error occurred", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_INPUT("VALIDATION_400", "Input payload constraints violated", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED("AUTH_401", "Full authentication details required to access resource", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("AUTH_403", "Access denied: insufficient permission hierarchy privileges", HttpStatus.FORBIDDEN),
    RESOURCE_NOT_FOUND("EMR_404", "EMR or study resource record not found", HttpStatus.NOT_FOUND),
    CONFLICT("EMR_409", "Conflict detected: duplicate EMR record study registration request", HttpStatus.CONFLICT);

    private final String code;
    private final String defaultMessage;
    private final HttpStatus status;

    ErrorCode(String code, String defaultMessage, HttpStatus status) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.status = status;
    }
}
