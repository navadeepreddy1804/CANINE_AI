package com.canineai.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Password reset credentials envelope")
public class ResetPasswordRequest {

    @NotBlank(message = "Reset token is required")
    @Schema(description = "Token received via password recovery request flows", example = "d94bfa20-410a-429a-8b1c-c760cdcae104")
    private String token;

    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "New password must be at least 8 characters long")
    @Schema(description = "Target security password", example = "NewSecurePassword123!")
    private String newPassword;
}
