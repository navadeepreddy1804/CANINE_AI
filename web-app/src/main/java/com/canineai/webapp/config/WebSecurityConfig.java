package com.canineai.webapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Web-app Spring Security configuration.
 *
 * The web-app is a Thymeleaf MVC client whose real authentication is handled
 * by the backend JWT layer. Spring Security here only needs to:
 *   - Permit public pages (login, register, static assets)
 *   - Require an active session for everything else
 *   - Disable CSRF (all mutating operations are proxied through the backend
 *     which enforces JWT; web-app sessions are HttpOnly cookies)
 *
 * Without this class, Spring Boot's default auto-configuration blocks every
 * POST request (including /register) with HTTP 403 FORBIDDEN because no CSRF
 * token is present in the form.
 */
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF — actual security is enforced by the backend JWT filter.
            // The web-app only holds a session cookie; all API mutations carry a Bearer token.
            .csrf(AbstractHttpConfigurer::disable)

            .authorizeHttpRequests(auth -> auth
                // Public pages — no session required
                .requestMatchers(
                    "/login",
                    "/register",
                    "/logout",
                    "/dashboard",
                    "/",
                    "/error"
                ).permitAll()

                // Static assets — always public
                .requestMatchers(
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/fonts/**",
                    "/favicon.ico",
                    "/webjars/**"
                ).permitAll()

                // All other routes require an authenticated session
                .anyRequest().authenticated()
            )

            // Use our own login page rather than Spring Security's generated one
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/spring-security-login") // Prevent intercepting POST /login
                .permitAll()
            )

            .logout(logout -> logout
                .logoutUrl("/spring-security-logout") // Prevent intercepting GET /logout
                .logoutSuccessUrl("/dashboard")
                .permitAll()
            );

        return http.build();
    }
}
