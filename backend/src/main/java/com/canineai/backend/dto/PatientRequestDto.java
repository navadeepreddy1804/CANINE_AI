package com.canineai.backend.dto;

import com.canineai.backend.entity.Gender;
import com.canineai.backend.entity.PatientStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDate;

@Data
public class PatientRequestDto {

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String hospitalPatientId;

    @NotBlank(message = "Full Name is required")
    private String fullName;

    @NotNull(message = "Date of Birth is required")
    @PastOrPresent(message = "Date of Birth cannot be in the future")
    private LocalDate dateOfBirth;

    @NotNull(message = "Gender is required")
    private Gender gender;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[0-9\\-\\s()]{7,20}$", message = "Invalid phone number format")
    private String phone;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    private String address;
    private String city;
    private String state;
    private String country;
    private String bloodGroup;
    private String medicalNotes;

    @NotBlank(message = "Orthodontist name is required")
    private String orthodontist;

    @NotBlank(message = "Hospital name is required")
    private String hospital;

    @NotNull(message = "Patient status is required")
    private PatientStatus status;
}
