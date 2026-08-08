package com.canineai.backend.service;

import com.canineai.backend.common.BusinessException;
import com.canineai.backend.dto.LoginRequest;
import com.canineai.backend.dto.LoginResponse;
import com.canineai.backend.dto.UserDto;
import com.canineai.backend.entity.Role;
import com.canineai.backend.entity.User;
import com.canineai.backend.mapper.UserMapper;
import com.canineai.backend.repository.AuditLogRepository;
import com.canineai.backend.repository.PasswordResetTokenRepository;
import com.canineai.backend.repository.RefreshTokenRepository;
import com.canineai.backend.repository.RoleRepository;
import com.canineai.backend.repository.UserRepository;
import com.canineai.backend.config.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider tokenProvider;
    @Mock
    private UserMapper userMapper;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                userRepository,
                refreshTokenRepository,
                passwordResetTokenRepository,
                auditLogRepository,
                passwordEncoder,
                tokenProvider,
                userMapper,
                roleRepository,
                emailService
            , eventPublisher
        );
    }

    @Test
        void loginBlocksDisabledAccountsWithFriendlyMessage() {
        User user = User.builder()
            .email("doctor@example.com")
            .password("encoded")
            .fullName("Dr. Example")
            .phone("+1 555 1212")
            .enabled(true)
            .accountLocked(false)
            .accountExpired(false)
            .roles(Set.of(Role.builder().name("ORTHODONTIST").build()))
            .build();

        when(userRepository.findByEmailActive("doctor@example.com")).thenReturn(Optional.of(user));

        // Mock password encoder to accept the provided plaintext password for the test
        when(passwordEncoder.matches("Secret123", user.getPassword())).thenReturn(true);
        // Mock token provider to avoid null tokens during login flow
        when(tokenProvider.generateToken(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString())).thenReturn("access-token");
        when(userMapper.toDto(user)).thenReturn(new UserDto());

        LoginRequest request = new LoginRequest();
        request.setEmail("doctor@example.com");
        request.setPassword("Secret123");

        // Login should not throw a forbidden exception now that verification gating is removed
        LoginResponse response = org.junit.jupiter.api.Assertions.assertDoesNotThrow(
            () -> authService.login(request, "127.0.0.1", "JUnit")
        );
        assertEquals("ORTHODONTIST", response.getUser().getRole());
        assertEquals("Orthodontist", response.getUser().getRoleTitle());
        assertEquals(Set.of("ORTHODONTIST"), response.getUser().getRoles());
    }

    @Test
    void registerSuccessfullyCreatesUserAndAssignsRole() {
        com.canineai.backend.dto.RegisterRequest request = new com.canineai.backend.dto.RegisterRequest();
        request.setFullName("Dr. Jane Smith");
        request.setUsername("janesmith");
        request.setEmail("jane@example.com");
        request.setPhone("+1 555 1234567");
        request.setPassword("Password123");

        when(userRepository.existsByEmailAndDeletedFalse("jane@example.com")).thenReturn(false);
        when(userRepository.existsByUsernameAndDeletedFalse("janesmith")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("encodedPassword");
        
        Role role = Role.builder().name("ORTHODONTIST").build();
        when(roleRepository.findByName("ORTHODONTIST")).thenReturn(Optional.of(role));
        
        User savedUser = User.builder()
                .id(1L)
                .email("jane@example.com")
                .username("janesmith")
                .fullName("Dr. Jane Smith")
                .phone("+1 555 1234567")
                .password("encodedPassword")
                .roles(Set.of(role))
                .build();
        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class))).thenReturn(savedUser);
        when(userMapper.toDto(savedUser)).thenReturn(new UserDto());

        UserDto result = authService.register(request, "127.0.0.1", "JUnit");
        assertEquals("ORTHODONTIST", result.getRole());
    }

    @Test
    void registerThrowsConflictWhenEmailExists() {
        com.canineai.backend.dto.RegisterRequest request = new com.canineai.backend.dto.RegisterRequest();
        request.setFullName("Dr. Duplicate");
        request.setUsername("duplicateuser");
        request.setEmail("duplicate@example.com");
        request.setPhone("+1 555 1234567");
        request.setPassword("Password123");

        when(userRepository.existsByEmailAndDeletedFalse("duplicate@example.com")).thenReturn(true);

        assertThrows(BusinessException.ConflictException.class, () -> 
            authService.register(request, "127.0.0.1", "JUnit")
        );
    }

    @Test
    void repeatedLoginReusesAndUpdatesExistingRefreshToken() {
        User user = User.builder()
                .id(2L)
                .email("doctor2@example.com")
                .password("encoded")
                .fullName("Dr. Second")
                .phone("+1 555 9999")
                .enabled(true)
                .roles(Set.of(Role.builder().name("ORTHODONTIST").build()))
                .build();

        when(userRepository.findByEmailActive("doctor2@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Secret123", user.getPassword())).thenReturn(true);
        when(tokenProvider.generateToken(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString())).thenReturn("access-token-2");
        when(userMapper.toDto(user)).thenReturn(new UserDto());

        com.canineai.backend.entity.RefreshToken existingToken = com.canineai.backend.entity.RefreshToken.builder()
                .id(10L)
                .token("old-token-guid")
                .user(user)
                .expiryDate(java.time.Instant.now().plusSeconds(3600))
                .revoked(false)
                .build();
        when(refreshTokenRepository.findAllByUser(user)).thenReturn(java.util.List.of(existingToken));

        LoginRequest request = new LoginRequest();
        request.setEmail("doctor2@example.com");
        request.setPassword("Secret123");

        LoginResponse response = authService.login(request, "127.0.0.1", "JUnit");
        org.junit.jupiter.api.Assertions.assertNotNull(response.getRefreshToken());
        org.junit.jupiter.api.Assertions.assertNotEquals("old-token-guid", response.getRefreshToken());
    }
}
