package com.canineai.backend.service;

import com.canineai.backend.common.BusinessException;
import com.canineai.backend.config.ai.ModelRegistry;
import com.canineai.backend.config.ai.ModelSelector;
import com.canineai.backend.dto.AiJobRequest;
import com.canineai.backend.dto.AiJobResponse;
import com.canineai.backend.dto.AiProgressResponse;
import com.canineai.backend.entity.AIJob;
import com.canineai.backend.entity.JobState;
import com.canineai.backend.repository.AIJobRepository;
import com.canineai.backend.repository.StudyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiJobServiceImpl implements AiJobService {

    private final AIJobRepository jobRepository;
    private final StudyRepository studyRepository;
    private final ModelSelector modelSelector;
    private final InferenceService inferenceService;

    @Override
    @Transactional
    public AiJobResponse submitJob(AiJobRequest request, String currentUser) {
        log.info("Submitting AI diagnostics job for Study ID: {}", request.getStudyId());

        // Check if study exists
        if (!studyRepository.existsById(request.getStudyId())) {
            throw new BusinessException.ResourceNotFoundException("Study not found: " + request.getStudyId());
        }

        // Idempotency: Return existing completed or active job if present
        java.util.Optional<AIJob> existingJob = jobRepository.findFirstByStudyIdAndDeletedFalseAndStateInOrderByCreatedAtDesc(
                request.getStudyId(),
                java.util.List.of(JobState.QUEUED, JobState.RUNNING, JobState.COMPLETED)
        );
        if (existingJob.isPresent()) {
            log.info("Returning existing AI job for Study ID: {} (State: {})", request.getStudyId(), existingJob.get().getState());
            return mapToResponse(existingJob.get());
        }

        // Dynamically select model based on task type
        ModelRegistry.ModelEndpoint endpoint = modelSelector.selectModel(request.getTaskType());

        AIJob job = AIJob.builder()
                .studyId(request.getStudyId())
                .taskType(request.getTaskType())
                .state(JobState.QUEUED)
                .activeModelName(endpoint.getName())
                .modelVersion(endpoint.getVersion())
                .progressPercentage(0)
                .build();
        
        job.setCreatedBy(currentUser);
        job.setCreatedAt(LocalDateTime.now());
        AIJob saved = jobRepository.save(job);

        // Transition study status to ANALYSIS_RUNNING upon AI Job creation
        com.canineai.backend.entity.Study study = studyRepository.findById(request.getStudyId())
                .orElseThrow(() -> new BusinessException.ResourceNotFoundException("Study not found: " + request.getStudyId()));
        study.setStatus(com.canineai.backend.entity.StudyStatus.ANALYSIS_RUNNING);
        studyRepository.save(study);
        log.info("Study {} status transitioned to ANALYSIS_RUNNING", study.getId());

        // Async execution of FastAPI inference
        inferenceService.triggerInference(saved.getId());

        return mapToResponse(saved);
    }

    @Override
    public AiJobResponse getJob(UUID jobId) {
        AIJob job = jobRepository.findByIdActive(jobId)
                .orElseThrow(() -> new BusinessException.ResourceNotFoundException("AI Job record not found"));
        return mapToResponse(job);
    }

    @Override
    public AiProgressResponse getProgress(UUID jobId) {
        AIJob job = jobRepository.findByIdActive(jobId)
                .orElseThrow(() -> new BusinessException.ResourceNotFoundException("AI Job record not found"));

        // Calculate dynamic stage progress details
        String stage = job.getCurrentStage() != null ? job.getCurrentStage() : resolveStage(job.getProgressPercentage());

        int timeRemainingSeconds = 0;
        long elapsedTimeSeconds = 0;
        if (job.getStartTime() != null) {
            LocalDateTime end = (job.getEndTime() != null) ? job.getEndTime() : LocalDateTime.now();
            elapsedTimeSeconds = java.time.temporal.ChronoUnit.SECONDS.between(job.getStartTime(), end);
            
            if (job.getState() == JobState.RUNNING && job.getProgressPercentage() > 0 && elapsedTimeSeconds > 0) {
                double speed = (double) job.getProgressPercentage() / elapsedTimeSeconds;
                timeRemainingSeconds = (int) ((100.0 - job.getProgressPercentage()) / speed);
            } else if (job.getState() == JobState.RUNNING) {
                timeRemainingSeconds = 120; // Default estimation
            }
        }

        return AiProgressResponse.builder()
                .jobId(jobId)
                .state(job.getState())
                .progressPercentage(job.getProgressPercentage())
                .currentStage(stage)
                .currentModel(job.getActiveModelName() + " (" + job.getModelVersion() + ")")
                .elapsedTimeSeconds(elapsedTimeSeconds)
                .timeRemainingSeconds(timeRemainingSeconds)
                .gpuUsagePercent(job.getState() == JobState.RUNNING ? 88 : 0)
                .cpuUsagePercent(job.getState() == JobState.RUNNING ? 12 : 0)
                .errorMessage(job.getErrorMessage())
                .build();
    }

    @Override
    @Transactional
    public void cancelJob(UUID jobId, String currentUser) {
        log.info("Cancelling active AI job: {}", jobId);
        AIJob job = jobRepository.findByIdActive(jobId)
                .orElseThrow(() -> new BusinessException.ResourceNotFoundException("AI Job record not found"));

        if (job.getState() == JobState.COMPLETED || job.getState() == JobState.FAILED) {
            throw new BusinessException.ConflictException("Job has already terminated");
        }

        job.setState(JobState.CANCELLED);
        job.setUpdatedBy(currentUser);
        job.setUpdatedAt(LocalDateTime.now());
        jobRepository.save(job);

        inferenceService.cancelInference(jobId);
    }

    @Override
    @Transactional
    public void deleteJob(UUID jobId, String currentUser) {
        log.info("Soft-deleting AI Job record: {}", jobId);
        AIJob job = jobRepository.findByIdActive(jobId)
                .orElseThrow(() -> new BusinessException.ResourceNotFoundException("AI Job record not found"));

        job.setDeleted(true);
        job.setUpdatedBy(currentUser);
        job.setUpdatedAt(LocalDateTime.now());
        jobRepository.save(job);
    }

    private String resolveStage(int percentage) {
        if (percentage <= 0) return "Queued";
        if (percentage < 20) return "Preprocessing Slices";
        if (percentage < 50) return "Segmentation Pipeline (ToothSeg)";
        if (percentage < 75) return "Localization Eruption Vectors";
        if (percentage < 99) return "Anatomical Measurements";
        return "Complete";
    }

    private AiJobResponse mapToResponse(AIJob j) {
        return AiJobResponse.builder()
                .id(j.getId())
                .studyId(j.getStudyId())
                .taskType(j.getTaskType())
                .state(j.getState())
                .activeModelName(j.getActiveModelName())
                .modelVersion(j.getModelVersion())
                .progressPercentage(j.getProgressPercentage())
                .resultJson(j.getResultJson())
                .errorMessage(j.getErrorMessage())
                .startTime(j.getStartTime())
                .endTime(j.getEndTime())
                .build();
    }
}
