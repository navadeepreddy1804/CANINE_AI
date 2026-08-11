package com.canineai.backend.repository;

import com.canineai.backend.entity.AnalysisHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AnalysisHistoryRepository extends JpaRepository<AnalysisHistory, UUID> {
    Optional<AnalysisHistory> findByStudyId(UUID studyId);
    List<AnalysisHistory> findAllByOrderByCompletedAtDesc();
    List<AnalysisHistory> findByCreatedByOrderByCompletedAtDesc(String createdBy);
}
