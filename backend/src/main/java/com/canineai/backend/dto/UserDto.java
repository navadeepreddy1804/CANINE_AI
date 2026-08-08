package com.canineai.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Schema(description = "Safe EMR profile view details of the authenticated clinician")
public class UserDto {

    @Schema(description = "Internal unique ID", example = "1")
    private Long id;

    @Schema(description = "Registered email address", example = "dr.darshan@metrodiagnostics.com")
    private String email;

    @Schema(description = "Clinician unique username", example = "janesmith")
    private String username;

    @Schema(description = "Full name of the clinician", example = "Dr. Darshan")
    private String fullName;

    @Schema(description = "Contact phone number", example = "+919876543210")
    private String phone;

    @Schema(description = "Clinician specialization title", example = "Chief Orthodontist")
    private String roleTitle;

    @Schema(description = "Primary authorization role assigned to the clinician", example = "ORTHODONTIST")
    private String role;

    @Schema(description = "Affiliated clinic/hospital location name", example = "Metro Dental Diagnostics")
    private String hospital;

    @Schema(description = "Assigned hospital department", example = "Orthodontics & Diagnostics")
    private String department;

    @Schema(description = "Medical registration license key", example = "MDR-2026-4290")
    private String medicalRegistrationNumber;

    @Schema(description = "Years of clinical practice experience", example = "5")
    private Integer yearsOfExperience;

    @Schema(description = "Blood group", example = "O+")
    private String bloodGroup;

    @Schema(description = "Account activation status flag", example = "true")
    private boolean enabled;

    @Schema(description = "Account creation timestamp", example = "2026-07-24T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Set of role privileges assigned to the EMR user", example = "[\"ROLE_ORTHODONTIST\"]")
    private Set<String> roles;

    @Schema(description = "Indicates whether the clinician profile requires mandatory missing fields to be completed", example = "true")
    private boolean profileComplete;
}
