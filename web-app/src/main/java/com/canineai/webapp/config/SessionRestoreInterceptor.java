package com.canineai.webapp.config;

import com.canineai.webapp.client.BackendClient;
import com.canineai.webapp.dto.UserDto;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

@Slf4j
@Component
public class SessionRestoreInterceptor implements HandlerInterceptor {

    @Autowired
    private BackendClient backendClient;

    private String formatDoctorName(String name) {
        if (name == null || name.isBlank()) return "";
        String trimmed = name.trim();
        if (trimmed.toLowerCase().startsWith("dr.")) {
            String stripped = trimmed.substring(3).trim();
            return "Dr. " + stripped;
        }
        return "Dr. " + trimmed;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession();

        // 1. Check if servlet session is unauthenticated
        if (session.getAttribute("authenticated") == null) {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("canineai-remember-me".equals(cookie.getName())) {
                        String refreshToken = cookie.getValue();
                        if (refreshToken != null && !refreshToken.isBlank() && !"token_value_placeholder".equals(refreshToken)) {
                            log.info("Found remember-me cookie. Attempting session restoration via token refresh...");
                            try {
                                // 2. Call backend to refresh token
                                Map<String, Object> tokenResponse = backendClient.refreshToken(refreshToken);
                                String newAccessToken = (String) tokenResponse.get("accessToken");
                                String newRefreshToken = (String) tokenResponse.get("refreshToken");

                                // 3. Re-save cookie for sliding expiration
                                Cookie rememberMeCookie = new Cookie("canineai-remember-me", newRefreshToken);
                                rememberMeCookie.setMaxAge(14 * 24 * 60 * 60); // 14 Days
                                rememberMeCookie.setPath("/");
                                rememberMeCookie.setHttpOnly(true);
                                rememberMeCookie.setSecure(request.isSecure());
                                response.addCookie(rememberMeCookie);

                                // 4. Fetch current user profile details
                                UserDto user = backendClient.getCurrentUser(newAccessToken);

                                // 5. Establish authenticated session
                                session.setAttribute("authenticated", true);
                                session.setAttribute("accessToken", newAccessToken);
                                session.setAttribute("refreshToken", newRefreshToken);
                                session.setAttribute("userEmail", user.getEmail());
                                session.setAttribute("doctorName", formatDoctorName(user.getFullName()));
                                session.setAttribute("organizationName", user.getHospital());
                                session.setAttribute("role", user.getRoleTitle());
                                session.setAttribute("department", user.getDepartment());
                                session.setMaxInactiveInterval(14 * 24 * 60 * 60); // 14 Days max inactive

                                log.info("Session restored successfully for user: {}", user.getEmail());
                            } catch (Exception e) {
                                log.warn("Automatic session restoration failed: {}. Clearing invalid remember-me cookie.", e.getMessage());
                                // Clear invalid/expired cookie
                                Cookie invalidCookie = new Cookie("canineai-remember-me", "");
                                invalidCookie.setMaxAge(0);
                                invalidCookie.setPath("/");
                                response.addCookie(invalidCookie);
                            }
                        }
                        break;
                    }
                }
            }
        }

        return true;
    }
}
