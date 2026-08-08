package com.canineai.webapp.dto;

import lombok.Data;

@Data
public class ForgotPasswordRequest {
    private String email;
    private String securityAnswer;
}
