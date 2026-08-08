package com.canineai.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Password recovery request envelope")
public class ForgotPasswordRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Schema(description = "Registered clinician email address", example = "dr.darshan@metrodiagnostics.com")
    private String email;

    @Schema(description = "Answer to the security question", example = "Smith")
    private String securityAnswer;
}
