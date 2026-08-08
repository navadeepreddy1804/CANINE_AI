package com.canineai.backend.service;

import com.canineai.backend.common.PagedResponse;
import com.canineai.backend.dto.PatientRequestDto;
import com.canineai.backend.dto.PatientResponseDto;
import com.canineai.backend.entity.Gender;
import com.canineai.backend.entity.PatientStatus;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.util.UUID;

public interface PatientService {

    /**
     * Registers a new patient EMR file in the database.
     * Checks duplicates on email and hospital ID.
     */
    PatientResponseDto createPatient(PatientRequestDto request, String currentUser);

    /**
     * Updates EMR attributes. Enforces optimistic lock checks.
     */
    PatientResponseDto updatePatient(UUID id, PatientRequestDto request, String currentUser);

    /**
     * Fetches Patient EMR profile by UUID. Enforces owner access check.
     */
    PatientResponseDto getPatient(UUID id, String currentUser);

    /**
     * Soft-deletes a patient EMR file. Enforces ownership check.
     */
    void deletePatient(UUID id, String currentUser);

    /**
     * Restores a soft-deleted or pending-deletion patient. Enforces ownership check.
     */
    PatientResponseDto restorePatient(UUID id, String currentUser);

    /**
     * Query matching multiple criteria with pagination. Filters by active user.
     */
    PagedResponse<PatientResponseDto> listPatients(
            Gender gender,
            LocalDate startDate,
            LocalDate endDate,
            String orthodontist,
            String hospital,
            PatientStatus status,
            Pageable pageable,
            String currentUser);

    /**
     * Dynamic search on IDs, Names, Phone, Attending Doctor. Filters by active user.
     */
    PagedResponse<PatientResponseDto> searchPatients(String query, Pageable pageable, String currentUser);
}
