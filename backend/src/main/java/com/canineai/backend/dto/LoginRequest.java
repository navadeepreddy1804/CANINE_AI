package com.canineai.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Credentials payload for clinician authentication")
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Schema(description = "Clinician registered email", example = "dr.darshan@metrodiagnostics.com")
    private String email;

    @NotBlank(message = "Password is required")
    @Schema(description = "Account security password", example = "SecurePassword123!")
    private String password;
}
