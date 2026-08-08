package com.canineai.backend.service;

import com.canineai.backend.entity.Patient;
import com.canineai.backend.entity.PatientStatus;
import com.canineai.backend.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PatientCleanupScheduler {

    private final PatientRepository patientRepository;

    /**
     * Finds and permanently deletes patients whose 3-day grace period has elapsed.
     * Scheduled to run once every hour.
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void permanentlyDeleteExpiredPatients() {
        log.info("Running Patient Cleanup Scheduler checking for expired soft-deleted patients...");
        LocalDateTime threshold = LocalDateTime.now();
        
        // Find all patients that are pending deletion
        List<Patient> patients = patientRepository.findAll().stream()
                .filter(p -> p.getStatus() == PatientStatus.PENDING_DELETION && !p.isDeleted())
                .filter(p -> p.getScheduledDeletionTime() != null && p.getScheduledDeletionTime().isBefore(threshold))
                .toList();

        if (!patients.isEmpty()) {
            log.info("Found {} patient(s) with expired soft-delete window. Setting permanent deleted status.", patients.size());
            for (Patient patient : patients) {
                patient.setDeleted(true);
                patient.setDeletedAt(LocalDateTime.now());
                patientRepository.save(patient);
                log.info("Patient permanently deleted: UUID={}", patient.getId());
            }
        }
    }
}
