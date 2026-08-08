package com.canineai.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response containing JWT bearer token details on authentication success")
public class LoginResponse {

    @Schema(description = "Access token header bearer parameter", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    @Schema(description = "Sliding refresh token parameter to renew sessions", example = "a2d1dca0-2ba4-411a-8b1c-c760cdcae104")
    private String refreshToken;

    @Schema(description = "EMR user profile details")
    private UserDto user;
}
