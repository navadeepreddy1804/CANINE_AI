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

    @Query("SELECT j FROM AIJob j WHERE j.state = 'QUEUED' AND j.deleted = false ORDER BY j.createdAt DESC")
    java.util.List<AIJob> findQueuedJobs(org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE AIJob j SET j.state = 'CLAIMED', j.workerId = :workerId, j.claimedAt = :claimedAt, j.leaseExpiry = :leaseExpiry, j.currentStage = 'CLAIMED', j.progressPercentage = 15 WHERE j.id = :jobId AND j.state = 'QUEUED' AND j.deleted = false")
    int claimJobAtomically(@Param("jobId") UUID jobId, @Param("workerId") String workerId, @Param("claimedAt") java.time.LocalDateTime claimedAt, @Param("leaseExpiry") java.time.LocalDateTime leaseExpiry);

    @Query("SELECT j FROM AIJob j WHERE j.deleted = false AND (j.state = 'CLAIMED' OR j.state = 'RUNNING') AND j.leaseExpiry < :now")
    java.util.List<AIJob> findStaleJobs(@Param("now") java.time.LocalDateTime now);
}
