package com.canineai.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Payload for Google Sign-In containing the Google ID Token")
public class GoogleAuthRequest {

    @NotBlank(message = "ID Token is required")
    @Schema(description = "The JWT ID Token provided by Google Identity Services", example = "eyJhbGciOiJSUzI1NiIs...")
    private String idToken;
}
