package com.canineai.backend.dto;

import com.canineai.backend.entity.Gender;
import com.canineai.backend.entity.PatientStatus;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class PatientResponseDto {
    private UUID id;
    private String hospitalPatientId;
    private String fullName;
    private LocalDate dateOfBirth;
    private int age;
    private Gender gender;
    private String phone;
    private String email;
    private String address;
    private String city;
    private String state;
    private String country;
    private String bloodGroup;
    private String medicalNotes;
    private String orthodontist;
    private String hospital;
    private LocalDate registrationDate;
    private PatientStatus status;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
