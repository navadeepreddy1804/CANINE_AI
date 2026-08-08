package com.canineai.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "User registration signup details request envelope")
public class RegisterRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Schema(description = "Clinician email address", example = "dr.darshan@metrodiagnostics.com")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    @Schema(description = "Account credential password (BCrypt encrypted at storage)", example = "SecurePassword123")
    private String password;

    @NotBlank(message = "Full name is required")
    @Schema(description = "Clinician full legal name", example = "Dr. Darshan Shah")
    private String fullName;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[0-9 .\\-()]{7,25}$", message = "Invalid phone number format")
    @Schema(description = "Contact phone number", example = "+1 555-0199")
    private String phone;

    @NotBlank(message = "Username is required")
    @Schema(description = "Clinician unique username", example = "janesmith")
    private String username;
    
    @NotBlank(message = "Security question is required")
    @Schema(description = "Security question for password recovery", example = "What is your mother's maiden name?")
    private String securityQuestion;
    
    @NotBlank(message = "Security answer is required")
    @Schema(description = "Security answer for password recovery", example = "Smith")
    private String securityAnswer;
}
