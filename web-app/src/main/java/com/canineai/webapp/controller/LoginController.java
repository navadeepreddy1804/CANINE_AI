package com.canineai.webapp.controller;

import com.canineai.webapp.client.BackendClient;
import com.canineai.webapp.dto.*;
import com.canineai.webapp.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Controller
public class LoginController {

    private final BackendClient backendClient;
    private final EmailService emailService;

    @org.springframework.beans.factory.annotation.Value("${canineai.google.client-id:}")
    private String googleClientId;

    public LoginController(BackendClient backendClient, EmailService emailService) {
        this.backendClient = backendClient;
        this.emailService = emailService;
    }

    private String formatDoctorName(String name) {
        if (name == null || name.isBlank()) return "";
        String trimmed = name.trim();
        if (trimmed.toLowerCase().startsWith("dr.")) {
            String stripped = trimmed.substring(3).trim();
            return "Dr. " + stripped;
        }
        return "Dr. " + trimmed;
    }

    @GetMapping("/")
    public String index(HttpSession session) {
        // Redirect directly to the showcase dashboard landing page
        return "redirect:/dashboard";
    }

    @GetMapping("/login")
    public String showLoginPage(org.springframework.ui.Model model, HttpSession session) {
        if (session.getAttribute("accessToken") != null) {
            return "redirect:/dashboard";
        }
        model.addAttribute("googleClientId", googleClientId != null ? googleClientId.trim() : "");
        return "login";
    }

    @PostMapping("/login")
    public String handleLogin(
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            @RequestParam(value = "rememberMe", required = false) boolean rememberMe,
            HttpSession session,
            HttpServletResponse response) {

        try {
            LoginRequest request = new LoginRequest();
            request.setEmail(username.trim());
            request.setPassword(password);

            LoginResponse loginResponse = backendClient.login(request);

            session.setAttribute("authenticated", true);
            session.setAttribute("accessToken", loginResponse.getAccessToken());
            session.setAttribute("refreshToken", loginResponse.getRefreshToken());
            session.setAttribute("userEmail", loginResponse.getUser().getEmail());
            session.setAttribute("doctorName", formatDoctorName(loginResponse.getUser().getFullName()));
            session.setAttribute("organizationName", loginResponse.getUser().getHospital());
            session.setAttribute("role", loginResponse.getUser().getRoleTitle());
            session.setAttribute("department", loginResponse.getUser().getDepartment());
            session.setMaxInactiveInterval(14 * 24 * 60 * 60); // 14 Days max inactive

            UserDto user = loginResponse.getUser();

            if (rememberMe) {
                Cookie rememberMeCookie = new Cookie("canineai-remember-me", loginResponse.getRefreshToken());
                rememberMeCookie.setMaxAge(14 * 24 * 60 * 60); // 14 Days as required!
                rememberMeCookie.setPath("/");
                rememberMeCookie.setHttpOnly(true);
                rememberMeCookie.setSecure(true);
                response.addCookie(rememberMeCookie);
            }

            try {
                emailService.sendActivityNotification(
                    user.getEmail(), 
                    "Clinician Sign In", 
                    "Successful log in recorded for " + formatDoctorName(user.getFullName())
                );
            } catch (Exception mailEx) {
                log.warn("Activity mail notification failed: {}", mailEx.getMessage());
            }

            return "redirect:/dashboard";
        } catch (Exception e) {
            log.error("Login authentication failure: {}", e.getMessage());
            return "redirect:/login?error=true";
        }
    }

    @PostMapping("/auth/google/callback")
    public String handleGoogleLogin(
            @RequestParam("credential") String idToken,
            HttpSession session,
            HttpServletResponse response) {
        
        try {
            LoginResponse loginResponse = backendClient.googleLogin(idToken);

            session.setAttribute("authenticated", true);
            session.setAttribute("accessToken", loginResponse.getAccessToken());
            session.setAttribute("refreshToken", loginResponse.getRefreshToken());
            session.setAttribute("userEmail", loginResponse.getUser().getEmail());
            session.setAttribute("doctorName", formatDoctorName(loginResponse.getUser().getFullName()));
            session.setAttribute("organizationName", loginResponse.getUser().getHospital());
            session.setAttribute("role", loginResponse.getUser().getRoleTitle());
            session.setAttribute("department", loginResponse.getUser().getDepartment());
            session.setMaxInactiveInterval(14 * 24 * 60 * 60);

            UserDto user = loginResponse.getUser();
            
            // Always set remember me for Google Sign-In
            Cookie rememberMeCookie = new Cookie("canineai-remember-me", loginResponse.getRefreshToken());
            rememberMeCookie.setMaxAge(14 * 24 * 60 * 60);
            rememberMeCookie.setPath("/");
            rememberMeCookie.setHttpOnly(true);
            rememberMeCookie.setSecure(true);
            response.addCookie(rememberMeCookie);

            if (!user.isProfileComplete()) {
                return "redirect:/complete-profile";
            }

            return "redirect:/dashboard";
        } catch (Exception e) {
            log.error("Google Login authentication failure: {}", e.getMessage());
            return "redirect:/login?googleError=true&googleMessage=" + encode(e.getMessage());
        }
    }

    @PostMapping("/register")
    public String handleRegister(
            @RequestParam("fullName") String fullName,
            @RequestParam("username") String username,
            @RequestParam("email") String email,
            @RequestParam(value = "phoneCode", required = false, defaultValue = "+91") String phoneCode,
            @RequestParam("phone") String phone,
            @RequestParam("password") String password,
            @RequestParam("confirmPassword") String confirmPassword,
            @RequestParam("securityQuestion") String securityQuestion,
            @RequestParam("securityAnswer") String securityAnswer,
            HttpSession session,
            HttpServletResponse response) {

        // ── Client-side guard re-check (server-side safety net) ───────────────
        if (fullName == null || fullName.isBlank()) {
            return "redirect:/login?registerError=true&registerMessage="
                    + encode("Full name is required.") + "#signup";
        }
        if (username == null || username.isBlank()) {
            return "redirect:/login?registerError=true&registerMessage="
                    + encode("Username is required.") + "#signup";
        }
        if (email == null || !email.contains("@")) {
            return "redirect:/login?registerError=true&registerMessage="
                    + encode("A valid email address is required.") + "#signup";
        }
        if (phone == null || phone.isBlank()) {
            return "redirect:/login?registerError=true&registerMessage="
                    + encode("Phone number is required.") + "#signup";
        }
        if (password == null || password.length() < 8) {
            return "redirect:/login?registerError=true&registerMessage="
                    + encode("Password must be at least 8 characters.") + "#signup";
        }
        if (!password.equals(confirmPassword)) {
            return "redirect:/login?registerError=true&registerMessage="
                    + encode("Passwords do not match.") + "#signup";
        }
        if (securityQuestion == null || securityQuestion.isBlank()) {
            return "redirect:/login?registerError=true&registerMessage="
                    + encode("Security question is required.") + "#signup";
        }
        if (securityAnswer == null || securityAnswer.isBlank()) {
            return "redirect:/login?registerError=true&registerMessage="
                    + encode("Security answer is required.") + "#signup";
        }

        try {
            String combinedPhone = phoneCode.trim() + " " + phone.trim();

            RegisterRequest request = new RegisterRequest();
            request.setFullName(fullName.trim());
            request.setUsername(username.trim());
            request.setEmail(email.trim());
            request.setPhone(combinedPhone);
            request.setPassword(password);
            request.setSecurityQuestion(securityQuestion.trim());
            request.setSecurityAnswer(securityAnswer.trim());

            backendClient.register(request);

            // Success — show banner on signup tab, JS will redirect to signin after delay
            return "redirect:/login?registered=true#signup";

        } catch (Exception e) {
            String raw = e.getMessage() != null ? e.getMessage() : "";
            log.error("Registration failure: {}", raw);

            // Map backend conflict/validation messages to friendly UI strings
            String friendly = mapToFriendlyError(raw);
            boolean isDuplicate = raw.contains("already registered")
                    || raw.contains("already in use")
                    || raw.contains("already exists");

            if (isDuplicate) {
                return "redirect:/login?registerError=true&registerMessage="
                        + encode(friendly) + "#signup";
            }
            return "redirect:/login?registerError=true&registerMessage="
                    + encode(friendly) + "#signup";
        }
    }

    /** URL-encodes a message for safe redirect param embedding. */
    private String encode(String msg) {
        return URLEncoder.encode(msg, StandardCharsets.UTF_8);
    }

    /**
     * Converts raw backend exception messages into short, friendly sentences.
     * Never exposes stack traces, class names, or SMTP errors to the user.
     */
    private String mapToFriendlyError(String raw) {
        if (raw == null || raw.isBlank()) return "Registration failed. Please try again.";
        String lower = raw.toLowerCase();

        if (lower.contains("email") && (lower.contains("already") || lower.contains("registered") || lower.contains("exists"))) {
            return "That email address is already registered. Please sign in or use a different email.";
        }
        if (lower.contains("username") && (lower.contains("already") || lower.contains("in use") || lower.contains("exists"))) {
            return "That username is already taken. Please choose a different username.";
        }
        if (lower.contains("password") && lower.contains("8")) {
            return "Password must be at least 8 characters and include an uppercase letter, a lowercase letter, and a digit.";
        }
        if (lower.contains("phone")) {
            return "Invalid phone number format.";
        }
        if (lower.contains("full name") || lower.contains("fullname")) {
            return "Full name is required.";
        }
        if (lower.contains("email") && lower.contains("required")) {
            return "A valid email address is required.";
        }
        if (lower.contains("username") && lower.contains("required")) {
            return "Username is required.";
        }
        if (lower.contains("connection") || lower.contains("refused") || lower.contains("timeout")) {
            return "The server is temporarily unavailable. Please try again in a moment.";
        }
        // Generic fallback — never expose raw Java/SMTP messages
        return "Registration failed. Please check your details and try again.";
    }

    @GetMapping("/forgot-password")
    public String showForgotPasswordPage() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password/question")
    public String handleGetSecurityQuestion(
            @RequestParam("email") String email,
            org.springframework.ui.Model model) {
        try {
            String question = backendClient.getSecurityQuestion(email);
            model.addAttribute("email", email);
            model.addAttribute("securityQuestion", question);
            return "forgot-password";
        } catch (Exception e) {
            log.error("Failed to fetch security question: {}", e.getMessage());
            model.addAttribute("error", "Failed to retrieve security question for this email.");
            return "forgot-password";
        }
    }

    @PostMapping("/forgot-password/verify")
    public String handleVerifySecurityQuestion(
            @RequestParam("email") String email,
            @RequestParam("securityQuestion") String securityQuestion,
            @RequestParam("securityAnswer") String securityAnswer,
            org.springframework.ui.Model model) {
        try {
            ForgotPasswordRequest request = new ForgotPasswordRequest();
            request.setEmail(email.trim());
            request.setSecurityAnswer(securityAnswer.trim());
            
            String token = backendClient.forgotPassword(request);
            
            model.addAttribute("resetToken", token);
            return "reset-password";
        } catch (Exception e) {
            log.error("Failed to verify security answer: {}", e.getMessage());
            model.addAttribute("email", email);
            model.addAttribute("securityQuestion", securityQuestion);
            model.addAttribute("error", "Incorrect answer. Please try again.");
            return "forgot-password";
        }
    }

    @PostMapping("/reset-password")
    public String handleResetPassword(
            @RequestParam("token") String token,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmPassword") String confirmPassword,
            org.springframework.ui.Model model) {
        
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("resetToken", token);
            model.addAttribute("error", "Passwords do not match.");
            return "reset-password";
        }

        try {
            ResetPasswordRequest request = new ResetPasswordRequest();
            request.setToken(token);
            request.setNewPassword(newPassword);
            
            backendClient.resetPassword(request);
            
            return "redirect:/login?resetSuccess=true";
        } catch (Exception e) {
            log.error("Failed to reset password: {}", e.getMessage());
            model.addAttribute("resetToken", token);
            model.addAttribute("error", "Failed to reset password. Token may have expired.");
            return "reset-password";
        }
    }

    @GetMapping("/complete-profile")
    public String showCompleteProfile(HttpSession session) {
        if (session.getAttribute("accessToken") == null) {
            return "redirect:/login";
        }
        return "complete-profile";
    }

    @PostMapping("/complete-profile")
    public String submitCompleteProfile(
            @RequestParam("phoneCode") String phoneCode,
            @RequestParam("phone") String phone,
            @RequestParam("roleTitle") String roleTitle,
            @RequestParam("hospital") String hospital,
            @RequestParam("medicalRegistrationNumber") String medicalRegistrationNumber,
            HttpSession session) {
        
        String accessToken = (String) session.getAttribute("accessToken");
        if (accessToken == null) {
            return "redirect:/login";
        }

        try {
            UserDto request = new UserDto();
            request.setPhone(phoneCode.trim() + " " + phone.trim());
            request.setRoleTitle(roleTitle.trim());
            request.setHospital(hospital.trim());
            request.setMedicalRegistrationNumber(medicalRegistrationNumber.trim());
            
            // Get current to not override name etc.
            UserDto currentUser = backendClient.getCurrentUser(accessToken);
            request.setFullName(currentUser.getFullName()); // Keep name
            request.setDepartment(currentUser.getDepartment());
            
            UserDto updatedUser = backendClient.updateProfile(request, accessToken);
            
            // Update session
            session.setAttribute("role", updatedUser.getRoleTitle());
            session.setAttribute("organizationName", updatedUser.getHospital());
            
            return "redirect:/dashboard";
        } catch (Exception e) {
            log.error("Complete Profile failed: {}", e.getMessage());
            return "redirect:/complete-profile?error=true&message=" + encode(e.getMessage());
        }
    }

    @GetMapping("/logout")
    public String handleLogout(HttpSession session, HttpServletResponse response) {
        String refreshToken = (String) session.getAttribute("refreshToken");
        String accessToken = (String) session.getAttribute("accessToken");
        if (refreshToken != null && accessToken != null) {
            try {
                backendClient.logout(refreshToken, accessToken);
            } catch (Exception e) {
                log.warn("Backend logout failed: {}", e.getMessage());
            }
        }

        session.invalidate();
        // Clear remember-me cookie
        Cookie cookie = new Cookie("canineai-remember-me", "");
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);

        return "redirect:/dashboard"; // Redirect to the landing showcase dashboard
    }
}
