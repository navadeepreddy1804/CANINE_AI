package com.canineai.backend.repository;

import com.canineai.backend.entity.AIJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AIJobRepository extends JpaRepository<AIJob, UUID> {

    @Query("SELECT j FROM AIJob j WHERE j.id = :id AND j.deleted = false")
    Optional<AIJob> findByIdActive(@Param("id") UUID id);

    Optional<AIJob> findFirstByStudyIdAndDeletedFalseOrderByEndTimeDesc(UUID studyId);

    Optional<AIJob> findFirstByStudyIdAndDeletedFalseAndStateInOrderByCreatedAtDesc(UUID studyId, java.util.List<com.canineai.backend.entity.JobState> states);
}
