package com.canineai.backend.integration;

import com.canineai.backend.common.ApiResponse;
import org.springframework.stereotype.Component;

@Component
public class ResponseDispatcher {

    /**
     * Standardizes outputs envelope packaging models.
     */
    public <T> ApiResponse<T> dispatchSuccess(T data, String message) {
        return ApiResponse.success(data, message);
    }

    public <T> ApiResponse<T> dispatchError(String errorMessage) {
        return ApiResponse.error(errorMessage);
    }
}
