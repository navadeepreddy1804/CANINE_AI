package com.canineai.backend.repository;

import com.canineai.backend.entity.ClinicalReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClinicalReportRepository extends JpaRepository<ClinicalReport, UUID> {

    boolean existsByStudyIdAndDeletedFalse(UUID studyId);

    /**
     * Lists active reports whose study belongs to a patient created by the supplied doctor.
     *
     * Callers must use this owner-scoped query when presenting reports to an authenticated user.
     */
    @Query("""
            SELECT r
            FROM ClinicalReport r, Study s
            WHERE r.studyId = s.id
              AND r.deleted = false
              AND s.patient.createdBy = :owner
            ORDER BY r.createdAt DESC
            """)
    List<ClinicalReport> findAllOwned(@Param("owner") String owner);

    /**
     * Loads an active report only when its study belongs to a patient created by the supplied doctor.
     */
    @Query("""
            SELECT r
            FROM ClinicalReport r, Study s
            WHERE r.id = :reportId
              AND r.studyId = s.id
              AND r.deleted = false
              AND s.patient.createdBy = :owner
            """)
    Optional<ClinicalReport> findByIdOwned(@Param("reportId") UUID reportId, @Param("owner") String owner);

    /**
     * Loads an active study report only when the study belongs to a patient created by the supplied doctor.
     */
    @Query("""
            SELECT r
            FROM ClinicalReport r, Study s
            WHERE r.studyId = :studyId
              AND r.studyId = s.id
              AND r.deleted = false
              AND s.patient.createdBy = :owner
            """)
    Optional<ClinicalReport> findByStudyIdOwned(@Param("studyId") UUID studyId, @Param("owner") String owner);
}
