package com.canineai.backend.common;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldReturnFriendlyMessageForValidationErrors() {
        BusinessException.ValidationException ex =
                new BusinessException.ValidationException("Invalid date of birth: 45.6618345}");

        org.springframework.mock.web.MockHttpServletRequest request = new org.springframework.mock.web.MockHttpServletRequest();
        ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid date of birth.");
    }

    @Test
    void shouldReturnFriendlyMessageForUnreadableRequestBody() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "Could not read document: Invalid date format",
                new MockHttpInputMessage("{}".getBytes())
        );

        org.springframework.mock.web.MockHttpServletRequest request = new org.springframework.mock.web.MockHttpServletRequest();
        ResponseEntity<ApiResponse<Void>> response = handler.handleHttpMessageNotReadable(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid date of birth.");
    }

    @Test
    void shouldHandleInternalServerErrorWithContext() {
        Exception ex = new RuntimeException("Database timeout");
        org.springframework.mock.web.MockHttpServletRequest request = new org.springframework.mock.web.MockHttpServletRequest();
        request.setRequestURI("/api/v1/patients/123/analysis");
        request.setMethod("POST");

        ResponseEntity<ApiResponse<Void>> response = handler.handleAllExceptions(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred. Please try again.");
    }
}

