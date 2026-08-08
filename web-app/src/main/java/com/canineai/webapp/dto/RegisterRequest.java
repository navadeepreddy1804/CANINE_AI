package com.canineai.webapp.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String email;
    private String password;
    private String fullName;
    private String username;
    private String phone;
    private String securityQuestion;
    private String securityAnswer;
    
}
