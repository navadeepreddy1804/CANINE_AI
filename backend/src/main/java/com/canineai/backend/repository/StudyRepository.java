package com.canineai.backend.repository;

import com.canineai.backend.entity.Study;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudyRepository extends JpaRepository<Study, UUID> {
    Optional<Study> findByStudyInstanceUidAndDeletedFalse(String studyInstanceUid);
    boolean existsByStudyInstanceUidAndDeletedFalse(String studyInstanceUid);
    java.util.List<Study> findByPatientIdAndDeletedFalse(UUID patientId);
    Optional<Study> findByUploadSessionIdAndDeletedFalse(UUID uploadSessionId);
}
