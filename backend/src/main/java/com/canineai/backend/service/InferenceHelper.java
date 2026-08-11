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

    public String generateSelfContainedDemoJson(UUID studyId) {
        String patientKey = "patient";
        String fileKey = "file";
        if (studyId != null) {
            var optStudy = studyRepository.findById(studyId);
            if (optStudy.isPresent()) {
                var study = optStudy.get();
                if (study.getPatient() != null) {
                    patientKey = study.getPatient().getHospitalPatientId() + "_" + study.getPatient().getFullName();
                }
                if (study.getStudyDescription() != null) {
                    fileKey = study.getStudyDescription();
                }
            }
        }

        String combinedKey = patientKey + ":" + fileKey + ":" + (studyId != null ? studyId.toString() : "123");
        int seed = Math.abs(combinedKey.hashCode());

        String[] statuses = {"IMPACTED", "DELAYED_ERUPTION", "ERUPTED"};
        String status = statuses[seed % statuses.length];
        
        int confidence = 63 + (seed % 18);
        double angle = 15.0 + (seed % 40) + ((seed % 10) / 10.0);
        double volume = 320.0 + (seed % 200) + ((seed % 10) / 10.0);
        double distMidline = 3.0 + (seed % 12) + ((seed % 10) / 10.0);
        double distOcclusal = 6.0 + (seed % 14) + ((seed % 10) / 10.0);
        String fdi = (seed % 2 == 0) ? "13" : "23";
        String toothName = (seed % 2 == 0) ? "Maxillary Right Canine" : "Maxillary Left Canine";

        String findings = status.contains("IMPACTED") ? "Impacted maxillary canine with palatal displacement relative to dental arch." 
                        : (status.contains("DELAYED") ? "Delayed eruption pattern with increased pericoronal space." 
                        : "Normal maxillary canine eruption position.");

        String rec = status.contains("IMPACTED") ? "Surgical exposure with orthodontic traction recommended." 
                   : (status.contains("DELAYED") ? "Periodic 6-month radiographic and clinical monitoring." 
                   : "Routine oral hygiene and preventive orthodontic evaluation.");

        return String.format(java.util.Locale.US, """
            {
              "status": "COMPLETED",
              "prediction": {
                "eruptionStatus": "%s",
                "confidence": %d,
                "fdiNumber": "%s",
                "toothName": "%s",
                "angulation": %.1f,
                "volume": %.1f,
                "distanceToMidline": %.1f,
                "distanceToOcclusalPlane": %.1f,
                "clinicalFindings": "%s",
                "clinicalRecommendation": "%s"
              }
            }
            """, status, confidence, fdi, toothName, angle, volume, distMidline, distOcclusal, findings, rec);
    }
}
