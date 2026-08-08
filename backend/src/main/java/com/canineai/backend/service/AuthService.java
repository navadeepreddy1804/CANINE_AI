package com.canineai.backend.service;

import com.canineai.backend.dto.*;

public interface AuthService {

    /**
     * Authenticates credentials, writes login success logs, and issues JWT tokens.
     * @param request Auth credentials.
     * @param ipAddress Caller IP.
     * @param deviceInfo Caller User-Agent.
     * @return Access and sliding expiration refresh tokens.
     */
    LoginResponse login(LoginRequest request, String ipAddress, String deviceInfo);

    /**
     * Authenticates via Google ID Token.
     * @param request The Google ID Token request.
     * @param ipAddress Caller IP.
     * @param deviceInfo Caller User-Agent.
     * @return Access and sliding expiration refresh tokens.
     */
    LoginResponse googleLogin(GoogleAuthRequest request, String ipAddress, String deviceInfo);

    /**
     * Invalidates the active refresh token and registers logout audit trace logs.
     * @param refreshToken Token to clear.
     * @param email Active context user email.
     */
    void logout(String refreshToken, String email);

    /**
     * Resolves a new access token if the provided refresh token is valid.
     * @param request Token package.
     * @return New tokens.
     */
    TokenResponse refresh(RefreshTokenRequest request);

    /**
     * Fetches current user profile from EMR database context.
     * @param email User context email.
     * @return User details DTO.
     */
    UserDto getCurrentUser(String email);

    /**
     * Updates user password inside EMR context.
     * @param email User context email.
     * @param request Pass update payload.
     */
    void changePassword(String email, ChangePasswordRequest request);

    /**
     * Generates a password reset token and logs request trace.
     * Requires the correct security answer.
     * @param request The forgot password request.
     * @return The generated reset token.
     */
    String forgotPassword(ForgotPasswordRequest request);

    /**
     * Fetches the security question for a given user email.
     * @param email Target clinician email.
     * @return The security question.
     */
    String getSecurityQuestion(String email);

    /**
     * Resets password using a validated reset token.
     * @param token Reset token.
     * @param newPassword Target password.
     */
    void resetPassword(String token, String newPassword);

    /**
     * Registers a new clinician profile in the database and returns details.
     * @param request Registration details.
     * @param ipAddress Caller IP.
     * @param deviceInfo Caller User-Agent.
     * @return User details DTO.
     */
    UserDto register(RegisterRequest request, String ipAddress, String deviceInfo);

    /**
     * Updates profile details of the clinician (name, phone, professional fields).
     */
    UserDto updateProfile(String email, UserDto dto);
}

