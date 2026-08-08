package com.canineai.backend.controller;

import com.canineai.backend.common.ApiResponse;
import com.canineai.backend.dto.*;
import com.canineai.backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication Interface", description = "Endpoints authorizing clinician login tokens and password updates")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Authenticate user credentials", description = "Verifies email and password, logs IP trace, and yields JWT bearer tokens.")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpServletRequest) {
        
        String ipAddress = httpServletRequest.getRemoteAddr();
        String deviceInfo = httpServletRequest.getHeader("User-Agent");
        
        LoginResponse response = authService.login(request, ipAddress, deviceInfo);
        return ResponseEntity.ok(ApiResponse.success(response, "Authentication successful"));
    }

    @PostMapping("/google")
    @Operation(summary = "Authenticate using Google ID Token", description = "Verifies the token, logs IP trace, and yields JWT bearer tokens. Automatically creates user if they do not exist.")
    public ResponseEntity<ApiResponse<LoginResponse>> googleLogin(
            @Valid @RequestBody GoogleAuthRequest request,
            HttpServletRequest httpServletRequest) {
        
        String ipAddress = httpServletRequest.getRemoteAddr();
        String deviceInfo = httpServletRequest.getHeader("User-Agent");
        
        LoginResponse response = authService.googleLogin(request, ipAddress, deviceInfo);
        return ResponseEntity.ok(ApiResponse.success(response, "Google authentication successful"));
    }

    @PostMapping("/register")
    @Operation(summary = "Register new clinician profile", description = "Checks email uniqueness and registers new user details in the EMR database.")
    public ResponseEntity<ApiResponse<UserDto>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpServletRequest) {
        
        String ipAddress = httpServletRequest.getRemoteAddr();
        String deviceInfo = httpServletRequest.getHeader("User-Agent");
        
        UserDto response = authService.register(request, ipAddress, deviceInfo);
        return ResponseEntity.status(201).body(ApiResponse.success(response, "Clinician registered successfully"));
    }

    @PostMapping("/logout")
    @Operation(summary = "Invalidate active login session", description = "Revokes refresh tokens from registry cache and logs logout audits.")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestParam("refreshToken") String refreshToken,
            Principal principal) {
        
        String email = principal != null ? principal.getName() : "Anonymous";
        authService.logout(refreshToken, email);
        return ResponseEntity.ok(ApiResponse.success(null, "Logout successful"));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Reissue access token", description = "Parses sliding expiration refresh tokens and issues fresh access headers.")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {
        
        TokenResponse response = authService.refresh(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Token refreshed successfully"));
    }

    @GetMapping("/me")
    @Operation(summary = "Fetch current profile details", description = "Extracts authenticated user context from security headers and queries EMR properties.")
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUser(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized context"));
        }
        UserDto response = authService.getCurrentUser(principal.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Profile retrieved successfully"));
    }

    @PutMapping("/profile")
    @Operation(summary = "Update user profile details", description = "Updates full name, phone, specialty (role title), hospital, department, registration number.")
    public ResponseEntity<ApiResponse<UserDto>> updateProfile(
            @RequestBody UserDto request,
            Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized context"));
        }
        UserDto response = authService.updateProfile(principal.getName(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Profile updated successfully"));
    }

    @PutMapping("/change-password")
    @Operation(summary = "Update account security credentials", description = "Verifies older hash records and writes newly encrypted BCrypt password strings.")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Principal principal) {
        
        if (principal == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized context"));
        }
        authService.changePassword(principal.getName(), request);
        return ResponseEntity.ok(ApiResponse.success(null, "Password changed successfully"));
    }

    @GetMapping("/security-question")
    @Operation(summary = "Get user security question", description = "Retrieves the security question for a given email address.")
    public ResponseEntity<ApiResponse<String>> getSecurityQuestion(
            @RequestParam("email") String email) {
        
        String question = authService.getSecurityQuestion(email);
        return ResponseEntity.ok(ApiResponse.success(question, "Security question retrieved successfully"));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Initiate password recovery", description = "Verifies EMR account email and security answer, then returns a recovery token.")
    public ResponseEntity<ApiResponse<String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        
        String token = authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success(token, "Password recovery token issued successfully"));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset credential password", description = "Verifies the recovery token and saves the new BCrypt password hash.")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        
        authService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success(null, "Password reset successfully"));
    }
}
