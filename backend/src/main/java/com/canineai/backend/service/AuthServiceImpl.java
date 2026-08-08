package com.canineai.backend.service;

import com.canineai.backend.common.BusinessException;
import com.canineai.backend.config.security.JwtTokenProvider;
import com.canineai.backend.dto.*;
import com.canineai.backend.entity.AuditLog;
import com.canineai.backend.entity.PasswordResetToken;
import com.canineai.backend.entity.RefreshToken;
import com.canineai.backend.entity.User;
import com.canineai.backend.event.WelcomeEmailEvent;
import com.canineai.backend.mapper.UserMapper;
import com.canineai.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String DEFAULT_ROLE = "ORTHODONTIST";
    private static final String DEFAULT_ROLE_TITLE = "Orthodontist";

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final UserMapper userMapper;
    private final RoleRepository roleRepository;
    private final EmailService emailService;
    private final ApplicationEventPublisher eventPublisher;

    @org.springframework.beans.factory.annotation.Value("${canineai.google.client-id:}")
    private String googleClientId;


    @Override
    @Transactional
    public LoginResponse login(LoginRequest request, String ipAddress, String deviceInfo) {
        log.info("Authentication attempt for user: {}", request.getEmail());

        // Check if user exists
        User user = userRepository.findByEmailActive(request.getEmail())
                .orElseGet(() -> {
                    // Log failure and throw invalid credentials
                    logFailedLogin(request.getEmail(), ipAddress, deviceInfo, "User not found");
                    throw new BusinessException.UnauthorizedException("Invalid email or password");
                });

        // Check locks and disabled states (allow login unless explicitly locked/expired)
        // Note: email verification requirement was removed — accounts are usable immediately after registration.
        if (user.isAccountLocked()) {
            logFailedLogin(request.getEmail(), ipAddress, deviceInfo, "Account locked");
            throw new BusinessException.ForbiddenException("Account is locked due to security parameters violations.");
        }

        // Validate password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            logFailedLogin(request.getEmail(), ipAddress, deviceInfo, "Password mismatch");
            throw new BusinessException.UnauthorizedException("Invalid email or password");
        }

        // Generate Access and Refresh tokens
        String activeRole = resolveActiveRole(user);
        String accessToken = tokenProvider.generateToken(user.getEmail(), activeRole);
        String refreshTokenString = issueRefreshToken(user);

        // Audit log success
        AuditLog successLog = AuditLog.builder()
                .email(user.getEmail())
                .action("LOGIN_SUCCESS")
                .ipAddress(ipAddress)
                .deviceInfo(deviceInfo)
                .timestamp(LocalDateTime.now())
                .details("Session authorized with JWT token")
                .build();
        auditLogRepository.save(successLog);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenString)
                .user(toCompleteUserDto(user, activeRole))
                .build();
    }

    @Override
    @Transactional
    public LoginResponse googleLogin(GoogleAuthRequest request, String ipAddress, String deviceInfo) {
        log.info("Google Sign-In authentication attempt");

        GoogleIdTokenVerifier.Builder verifierBuilder = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory());
        if (googleClientId != null && !googleClientId.isBlank()) {
            verifierBuilder.setAudience(java.util.Collections.singletonList(googleClientId));
        }
        GoogleIdTokenVerifier verifier = verifierBuilder.build();

        GoogleIdToken idToken;
        try {
            idToken = verifier.verify(request.getIdToken());
            if (idToken == null) {
                throw new BusinessException.UnauthorizedException("Invalid Google ID token");
            }
        } catch (Exception e) {
            log.error("Google ID Token verification failed", e);
            throw new BusinessException.UnauthorizedException("Failed to verify Google identity");
        }

        Payload payload = idToken.getPayload();
        String email = payload.getEmail();
        String name = (String) payload.get("name");

        User user = userRepository.findByEmailActive(email).orElse(null);
        if (user == null) {
            log.info("Creating new user from Google Sign-In: {}", email);
            
            com.canineai.backend.entity.Role userRole = roleRepository.findByName(DEFAULT_ROLE)
                    .orElseGet(() -> roleRepository.save(
                            com.canineai.backend.entity.Role.builder()
                                    .name(DEFAULT_ROLE)
                                    .description("Orthodontic specialist dashboard access role")
                                    .build()));

            java.util.Set<com.canineai.backend.entity.Role> roles = new java.util.HashSet<>();
            roles.add(userRole);

            String randomPassword = UUID.randomUUID().toString() + "Gg!1"; // Ensure it meets strength requirements temporarily
            String placeholderPhone = ""; // User will fill this in Complete Profile

            user = User.builder()
                    .email(email)
                    .username(email.split("@")[0] + "_" + UUID.randomUUID().toString().substring(0, 5)) // random unique
                    .password(passwordEncoder.encode(randomPassword))
                    .fullName(name != null ? name : "Google User")
                    .phone(placeholderPhone)
                    .roleTitle(DEFAULT_ROLE_TITLE)
                    .enabled(true)
                    .accountLocked(false)
                    .accountExpired(false)
                    .roles(roles)
                    .build();

            user = userRepository.save(user);

            AuditLog signupLog = AuditLog.builder()
                    .email(email)
                    .action("USER_SIGNUP_GOOGLE")
                    .ipAddress(ipAddress)
                    .deviceInfo(deviceInfo)
                    .timestamp(LocalDateTime.now())
                    .details("Clinician account created via Google Sign-In")
                    .build();
            auditLogRepository.save(signupLog);
            
            eventPublisher.publishEvent(new WelcomeEmailEvent(user.getEmail(), user.getFullName()));
        } else {
            if (user.isAccountLocked()) {
                logFailedLogin(email, ipAddress, deviceInfo, "Account locked (Google)");
                throw new BusinessException.ForbiddenException("Account is locked due to security parameters violations.");
            }
        }

        String activeRole = resolveActiveRole(user);
        String accessToken = tokenProvider.generateToken(user.getEmail(), activeRole);
        String refreshTokenString = issueRefreshToken(user);

        AuditLog successLog = AuditLog.builder()
                .email(user.getEmail())
                .action("LOGIN_SUCCESS_GOOGLE")
                .ipAddress(ipAddress)
                .deviceInfo(deviceInfo)
                .timestamp(LocalDateTime.now())
                .details("Session authorized with Google OAuth")
                .build();
        auditLogRepository.save(successLog);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenString)
                .user(toCompleteUserDto(user, activeRole))
                .build();
    }

    private String issueRefreshToken(User user) {
        String refreshTokenString = UUID.randomUUID().toString();

        java.util.List<RefreshToken> existingTokens = refreshTokenRepository.findAllByUser(user);
        RefreshToken primaryToken = null;
        if (!existingTokens.isEmpty()) {
            primaryToken = existingTokens.get(0);
            if (existingTokens.size() > 1) {
                for (int i = 1; i < existingTokens.size(); i++) {
                    refreshTokenRepository.delete(existingTokens.get(i));
                }
                refreshTokenRepository.flush();
            }
        }

        if (primaryToken == null) {
            primaryToken = RefreshToken.builder()
                    .token(refreshTokenString)
                    .user(user)
                    .expiryDate(Instant.now().plusMillis(86400000L * 7)) // 7 days
                    .revoked(false)
                    .build();
        } else {
            primaryToken.setToken(refreshTokenString);
            primaryToken.setExpiryDate(Instant.now().plusMillis(86400000L * 7));
            primaryToken.setRevoked(false);
        }

        refreshTokenRepository.saveAndFlush(primaryToken);
        return refreshTokenString;
    }

    @Override
    @Transactional
    public void logout(String refreshToken, String email) {
        log.info("Invalidating login token session for user: {}", email);
        
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenRepository.findByToken(refreshToken).ifPresent(token -> {
                token.setRevoked(true);
                refreshTokenRepository.saveAndFlush(token);
            });
        }

        if (email != null && !email.isBlank()) {
            userRepository.findByEmailActive(email).ifPresent(user -> {
                java.util.List<RefreshToken> tokens = refreshTokenRepository.findAllByUser(user);
                for (RefreshToken t : tokens) {
                    t.setRevoked(true);
                }
                refreshTokenRepository.saveAllAndFlush(tokens);
            });
        }

        AuditLog logoutLog = AuditLog.builder()
                .email(email != null ? email : "Anonymous")
                .action("LOGOUT")
                .timestamp(LocalDateTime.now())
                .details("Session invalidated by clinician request")
                .build();
        auditLogRepository.save(logoutLog);
    }

    @Override
    @Transactional
    public TokenResponse refresh(RefreshTokenRequest request) {
        RefreshToken token = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new BusinessException.UnauthorizedException("Invalid refresh token"));

        if (token.isRevoked() || token.getExpiryDate().isBefore(Instant.now())) {
            throw new BusinessException.UnauthorizedException("Expired or revoked refresh token. Please re-authenticate.");
        }

        User user = token.getUser();
        String activeRole = resolveActiveRole(user);
        String newAccessToken = tokenProvider.generateToken(user.getEmail(), activeRole);

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(token.getToken())
                .build();
    }

    @Override
    public UserDto getCurrentUser(String email) {
        User user = userRepository.findByEmailActive(email)
                .orElseThrow(() -> new BusinessException.ResourceNotFoundException("EMR Profile not found"));
        return toCompleteUserDto(user, resolveActiveRole(user));
    }

    @Override
    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmailActive(email)
                .orElseThrow(() -> new BusinessException.ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException.UnauthorizedException("Old password does not match original credentials");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Password successfully changed for user: {}", email);
    }

    @Override
    public String getSecurityQuestion(String email) {
        User user = userRepository.findByEmailActive(email)
                .orElseThrow(() -> new BusinessException.ResourceNotFoundException("User not found with email: " + email));
        if (user.getSecurityQuestion() == null || user.getSecurityQuestion().isBlank()) {
            throw new BusinessException.ValidationException("Security question not set for this account. Please contact support.");
        }
        return user.getSecurityQuestion();
    }

    @Override
    @Transactional
    public String forgotPassword(ForgotPasswordRequest request) {
        log.info("Password reset request initiated for email: {}", request.getEmail());
        User user = userRepository.findByEmailActive(request.getEmail())
                .orElseThrow(() -> new BusinessException.ResourceNotFoundException("User not found with email: " + request.getEmail()));

        if (user.getSecurityAnswer() == null || user.getSecurityAnswer().isBlank()) {
            throw new BusinessException.ValidationException("Security question not set for this account.");
        }

        if (request.getSecurityAnswer() == null || !request.getSecurityAnswer().trim().equalsIgnoreCase(user.getSecurityAnswer())) {
            throw new BusinessException.UnauthorizedException("Incorrect security answer");
        }

        // Expire older reset tokens
        passwordResetTokenRepository.deleteByUser(user);

        String resetToken = UUID.randomUUID().toString();
        PasswordResetToken tokenEntity = PasswordResetToken.builder()
                .token(resetToken)
                .user(user)
                .expiryDate(Instant.now().plusMillis(3600000)) // 1 hour expiration
                .build();

        passwordResetTokenRepository.save(tokenEntity);

        // Audit trace logging
        AuditLog forgotLog = AuditLog.builder()
                .email(request.getEmail())
                .action("FORGOT_PASSWORD_REQUEST")
                .timestamp(LocalDateTime.now())
                .details("Issued recovery token: " + resetToken)
                .build();
        auditLogRepository.save(forgotLog);

        log.info("Password recovery token issued for user: {}", resetToken);
        return resetToken;
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        log.info("Verifying password reset token");
        PasswordResetToken tokenEntity = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException.UnauthorizedException("Invalid or unrecognized password reset token"));

        if (tokenEntity.getExpiryDate().isBefore(Instant.now())) {
            throw new BusinessException.UnauthorizedException("Password reset token has expired");
        }

        User user = tokenEntity.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Delete the used token
        passwordResetTokenRepository.delete(tokenEntity);

        // Audit trace logging
        AuditLog resetLog = AuditLog.builder()
                .email(user.getEmail())
                .action("RESET_PASSWORD_SUCCESS")
                .timestamp(LocalDateTime.now())
                .details("Credentials updated via reset token")
                .build();
        auditLogRepository.save(resetLog);

        log.info("Password successfully updated via reset token for user: {}", user.getEmail());
    }

    @Override
    @Transactional
    public UserDto register(RegisterRequest request, String ipAddress, String deviceInfo) {
        log.info("Clinician signup registration initiated for email: {}", request.getEmail());

        // ── 1. Field presence validation ──────────────────────────────────────
        if (request.getFullName() == null || request.getFullName().isBlank()) {
            throw new BusinessException.ValidationException("Full name is required");
        }
        if (request.getEmail() == null || request.getEmail().isBlank() || !request.getEmail().contains("@")) {
            throw new BusinessException.ValidationException("A valid email address is required");
        }
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new BusinessException.ValidationException("Username is required");
        }
        if (request.getPhone() == null || request.getPhone().isBlank()) {
            throw new BusinessException.ValidationException("Phone number is required");
        }

        // ── 2. Duplicate checks ───────────────────────────────────────────────
        if (userRepository.existsByEmailAndDeletedFalse(request.getEmail().trim())) {
            throw new BusinessException.ConflictException("Email address is already registered");
        }
        if (userRepository.existsByUsernameAndDeletedFalse(request.getUsername().trim())) {
            throw new BusinessException.ConflictException("Username is already in use");
        }

        // ── 3. Password strength validation ───────────────────────────────────
        String pw = request.getPassword();
        if (pw == null || pw.length() < 8
                || !pw.matches(".*[A-Z].*")
                || !pw.matches(".*[a-z].*")
                || !pw.matches(".*\\d.*")) {
            throw new BusinessException.ValidationException(
                    "Password must be at least 8 characters and include an uppercase letter, a lowercase letter, and a digit");
        }

        // ── 4. Phone format validation ────────────────────────────────────────
        if (!request.getPhone().matches("^\\+?[0-9 .\\-()]{7,25}$")) {
            throw new BusinessException.ValidationException("Invalid phone number format");
        }

        // ── 5. Resolve default role (ORTHODONTIST) ────────────────────────────
        com.canineai.backend.entity.Role userRole = roleRepository.findByName(DEFAULT_ROLE)
                .orElseGet(() -> roleRepository.save(
                        com.canineai.backend.entity.Role.builder()
                                .name(DEFAULT_ROLE)
                                .description("Orthodontic specialist dashboard access role")
                                .build()));

        java.util.Set<com.canineai.backend.entity.Role> roles = new java.util.HashSet<>();
        roles.add(userRole);

        // ── 6. Build and persist the new user (active immediately) ────────────
        User user = User.builder()
                .email(request.getEmail().trim())
                .username(request.getUsername().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .phone(request.getPhone().trim())
                .roleTitle(DEFAULT_ROLE_TITLE)
                .securityQuestion(request.getSecurityQuestion())
                .securityAnswer(request.getSecurityAnswer() != null ? request.getSecurityAnswer().trim().toLowerCase() : null)
                .enabled(true)
                .accountLocked(false)
                .accountExpired(false)
                .roles(roles)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User persisted to MySQL with id={} email={}", savedUser.getId(), savedUser.getEmail());

        // ── 7. Audit trace ────────────────────────────────────────────────────
        AuditLog signupLog = AuditLog.builder()
                .email(savedUser.getEmail())
                .action("USER_SIGNUP")
                .ipAddress(ipAddress)
                .deviceInfo(deviceInfo)
                .timestamp(LocalDateTime.now())
                .details("Clinician account created successfully")
                .build();
        auditLogRepository.save(signupLog);

        // ── 8. Publish WelcomeEmailEvent — fires AFTER this transaction commits ──
        // WelcomeEmailListener picks this up on the @Async thread pool only after
        // MySQL has durably committed the new user row. Any failure inside the
        // listener is caught, logged, and discarded — registration is unaffected.
        eventPublisher.publishEvent(new WelcomeEmailEvent(savedUser.getEmail(), savedUser.getFullName()));

        log.info("Registration completed successfully for: {}", savedUser.getEmail());
        return toCompleteUserDto(savedUser, DEFAULT_ROLE);
    }

    @Override
    @Transactional
    public UserDto updateProfile(String email, UserDto dto) {
        log.info("Clinician profile update requested for: {}", email);
        User user = userRepository.findByEmailActive(email)
                .orElseThrow(() -> new BusinessException.ResourceNotFoundException("User not found with email: " + email));

        user.setFullName(dto.getFullName());
        user.setPhone(dto.getPhone());
        user.setRoleTitle(dto.getRoleTitle());
        user.setHospital(dto.getHospital());
        user.setDepartment(dto.getDepartment());
        user.setMedicalRegistrationNumber(dto.getMedicalRegistrationNumber());
        if (dto.getYearsOfExperience() != null) {
            user.setYearsOfExperience(dto.getYearsOfExperience());
        }
        if (dto.getBloodGroup() != null) {
            user.setBloodGroup(dto.getBloodGroup());
        }

        User saved = userRepository.save(user);
        return toCompleteUserDto(saved, resolveActiveRole(saved));
    }

    private void logFailedLogin(String email, String ip, String device, String reason) {
        AuditLog failLog = AuditLog.builder()
                .email(email)
                .action("LOGIN_FAILED")
                .ipAddress(ip)
                .deviceInfo(device)
                .timestamp(LocalDateTime.now())
                .details("Failed. Reason: " + reason)
                .build();
        auditLogRepository.save(failLog);
    }

    private UserDto toCompleteUserDto(User user, String activeRole) {
        UserDto dto = userMapper.toDto(user);
        if (dto == null) {
            throw new IllegalStateException("Unable to create authenticated user profile");
        }
        if (dto.getRoleTitle() == null || dto.getRoleTitle().isBlank()) {
            dto.setRoleTitle(toDisplayRole(activeRole));
        }
        if (dto.getRole() == null || dto.getRole().isBlank()) {
            dto.setRole(activeRole);
        }
        if (dto.getRoles() == null || dto.getRoles().isEmpty()) {
            dto.setRoles(java.util.Set.of(activeRole));
        }
        
        boolean isProfileComplete = dto.getPhone() != null && !dto.getPhone().isBlank()
                && dto.getHospital() != null && !dto.getHospital().isBlank()
                && dto.getRoleTitle() != null && !dto.getRoleTitle().isBlank()
                && dto.getMedicalRegistrationNumber() != null && !dto.getMedicalRegistrationNumber().isBlank();
        dto.setProfileComplete(isProfileComplete);
        
        return dto;
    }

    private String resolveActiveRole(User user) {
        return user.getRoles() == null || user.getRoles().isEmpty()
                ? DEFAULT_ROLE
                : user.getRoles().iterator().next().getName();
    }

    private String toDisplayRole(String role) {
        return DEFAULT_ROLE.equals(role) ? DEFAULT_ROLE_TITLE : role;
    }
}
