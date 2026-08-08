package com.canineai.backend.repository;

import com.canineai.backend.entity.StudyStorage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudyStorageRepository extends JpaRepository<StudyStorage, UUID> {
    Optional<StudyStorage> findByStudyId(UUID studyId);
    Optional<StudyStorage> findByUploadSessionId(UUID uploadSessionId);
}
