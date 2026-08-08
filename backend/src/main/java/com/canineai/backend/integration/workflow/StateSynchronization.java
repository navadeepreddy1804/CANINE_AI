package com.canineai.backend.integration.workflow;

import com.canineai.backend.entity.AIJob;
import com.canineai.backend.entity.JobState;
import com.canineai.backend.repository.AIJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class StateSynchronization {

    private final AIJobRepository jobRepository;

    /**
     * Synchronizes job status from FastAPI/AI Gateway directly into the EMR database state.
     */
    @Transactional
    public void syncAiJobState(UUID jobId, JobState newState, int progress, String resultJson) {
        log.info("Synchronizing AI Job state: {} -> State: {}, Progress: {}%", jobId, newState, progress);
        
        AIJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("AI Job not found: " + jobId));
        
        job.setState(newState);
        job.setProgressPercentage(progress);
        if (resultJson != null) {
            job.setResultJson(resultJson);
        }
        job.setUpdatedAt(LocalDateTime.now());
        job.setUpdatedBy("StateSynchronizer");
        
        jobRepository.save(job);
        log.debug("AI Job database synchronization complete.");
    }

}
