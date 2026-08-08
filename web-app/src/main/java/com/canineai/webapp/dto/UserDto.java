package com.canineai.webapp.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Set;

@Data
public class UserDto {
    private Long id;
    private String email;
    private String username;
    private String fullName;
    private String phone;
    private String roleTitle;
    private String role;
    private String hospital;
    private String department;
    private String medicalRegistrationNumber;
    private Integer yearsOfExperience;
    private String bloodGroup;
    private boolean enabled;
    private LocalDateTime createdAt;
    private Set<String> roles;
    private boolean profileComplete;
}
