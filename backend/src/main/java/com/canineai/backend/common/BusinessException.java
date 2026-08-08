package com.canineai.backend.common;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public static class ResourceNotFoundException extends BusinessException {
        public ResourceNotFoundException(String message) {
            super(ErrorCode.RESOURCE_NOT_FOUND, message);
        }
        public ResourceNotFoundException() {
            super(ErrorCode.RESOURCE_NOT_FOUND);
        }
    }

    public static class UnauthorizedException extends BusinessException {
        public UnauthorizedException(String message) {
            super(ErrorCode.UNAUTHORIZED, message);
        }
        public UnauthorizedException() {
            super(ErrorCode.UNAUTHORIZED);
        }
    }

    public static class ForbiddenException extends BusinessException {
        public ForbiddenException(String message) {
            super(ErrorCode.FORBIDDEN, message);
        }
        public ForbiddenException() {
            super(ErrorCode.FORBIDDEN);
        }
    }

    public static class ConflictException extends BusinessException {
        public ConflictException(String message) {
            super(ErrorCode.CONFLICT, message);
        }
        public ConflictException() {
            super(ErrorCode.CONFLICT);
        }
    }

    public static class ValidationException extends BusinessException {
        public ValidationException(String message) {
            super(ErrorCode.INVALID_INPUT, message);
        }
        public ValidationException() {
            super(ErrorCode.INVALID_INPUT);
        }
    }
}
