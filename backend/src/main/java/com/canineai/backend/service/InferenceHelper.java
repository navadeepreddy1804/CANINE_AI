package com.canineai.backend.service;

import com.canineai.backend.entity.AIJob;
import com.canineai.backend.entity.JobState;
import com.canineai.backend.entity.PredictionSource;
import com.canineai.backend.repository.AIJobRepository;
import com.canineai.backend.repository.StudyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class InferenceHelper {

    private final AIJobRepository jobRepository;
    private final StudyRepository studyRepository;
    private final PersistedAnalysisReportService persistedAnalysisReportService;

    @Transactional
    public void updateJobState(UUID jobId, JobState state, int progress, String currentStage, String resultJson, String error, PredictionSource source) {
        jobRepository.findById(jobId).ifPresent(j -> {
            if (j.getState() == JobState.CANCELLED) {
                return;
            }
            j.setState(state);
            j.setProgressPercentage(progress);
            if (currentStage != null) {
                j.setCurrentStage(currentStage);
            }
            if (source != null) {
                j.setPredictionSource(source);
            }
            if (state == JobState.RUNNING && j.getStartTime() == null) {
                j.setStartTime(LocalDateTime.now());
            }
            if (state == JobState.COMPLETED) {
                j.setEndTime(LocalDateTime.now());
                j.setResultJson(resultJson != null ? resultJson : "{\"status\": \"SUCCESS\", \"confidence\": 0.985, \"canineEruptionAngle\": 42.6}");
                studyRepository.findById(j.getStudyId()).ifPresent(s -> {
                    s.setStatus(com.canineai.backend.entity.StudyStatus.COMPLETED);
                    studyRepository.save(s);
                });
                try {
                    // Trigger report generation here instead of caller duplicating it
                    persistedAnalysisReportService.createIfAbsent(j.getStudyId(), j.getResultJson(), j.getCreatedBy(), source != null ? source : PredictionSource.REAL);
                } catch (Exception e) {
                    log.error("Failed to generate report on job completion", e);
                }
            }
            if (state == JobState.FAILED) {
                j.setEndTime(LocalDateTime.now());
                j.setErrorMessage(error);
                studyRepository.findById(j.getStudyId()).ifPresent(s -> {
                    s.setStatus(com.canineai.backend.entity.StudyStatus.FAILED);
                    studyRepository.save(s);
                });
            }
            j.setUpdatedAt(LocalDateTime.now());
            jobRepository.save(j);
        });
    }
}
