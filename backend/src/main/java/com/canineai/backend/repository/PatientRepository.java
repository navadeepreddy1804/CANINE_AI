package com.canineai.backend.repository;

import com.canineai.backend.entity.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID>, JpaSpecificationExecutor<Patient> {

    @Query("SELECT p FROM Patient p WHERE p.id = :id AND p.deleted = false")
    Optional<Patient> findByIdActive(@Param("id") UUID id);

    @Query("SELECT MAX(CAST(SUBSTRING(p.hospitalPatientId, 4) AS int)) FROM Patient p WHERE p.hospitalPatientId LIKE 'PT-%'")
    Integer findMaxHospitalPatientIdSequence();

    Optional<Patient> findByHospitalPatientIdAndDeletedFalse(String hospitalPatientId);

    Optional<Patient> findByHospitalPatientIdAndCreatedByAndDeletedFalse(String hospitalPatientId, String createdBy);

    /** Checks within a clinician's own patients only — mirrors the composite unique constraint. */
    boolean existsByHospitalPatientIdAndCreatedByAndDeletedFalse(String hospitalPatientId, String createdBy);

    /** Legacy full-table check used only when createdBy is unknown (system operations). */
    boolean existsByHospitalPatientIdAndDeletedFalse(String hospitalPatientId);

    /** Checks ALL rows including soft-deleted — used by ID generator to avoid reuse. */
    boolean existsByHospitalPatientId(String hospitalPatientId);

    boolean existsByEmailAndCreatedByAndDeletedFalse(String email, String createdBy);

    boolean existsByPhoneAndCreatedByAndDeletedFalse(String phone, String createdBy);

    // Dynamic filtering interface returning pagination pages
    default Page<Patient> findAllActive(Specification<Patient> spec, Pageable pageable) {
        Specification<Patient> activeSpec = (root, query, cb) -> cb.equal(root.get("deleted"), false);
        Specification<Patient> finalSpec = spec == null ? activeSpec : activeSpec.and(spec);
        return findAll(finalSpec, pageable);
    }
}
