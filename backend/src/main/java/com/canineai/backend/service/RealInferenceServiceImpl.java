package com.canineai.backend.service;

import com.canineai.backend.config.ai.ModelRegistry;
import com.canineai.backend.config.ai.ModelSelector;
import com.canineai.backend.entity.AIJob;
import com.canineai.backend.entity.JobState;
import com.canineai.backend.entity.PredictionSource;
import com.canineai.backend.repository.AIJobRepository;
import com.canineai.backend.repository.StudyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import java.time.Duration;
import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Service
@org.springframework.context.annotation.Primary
@RequiredArgsConstructor
public class RealInferenceServiceImpl implements InferenceService {

    private final AIJobRepository jobRepository;
    private final StudyRepository studyRepository;
    private final com.canineai.backend.repository.StudyStorageRepository studyStorageRepository;
    private final ModelSelector modelSelector;
    private final WebClient aiWebClient;
    private final InferenceHelper inferenceHelper;
    
    private final ExecutorService executorService = Executors.newFixedThreadPool(4);
    private final Map<UUID, Future<?>> activeJobs = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @org.springframework.beans.factory.annotation.Value("${canineai.ai.mode:real}")
    private String aiMode;

    @org.springframework.beans.factory.annotation.Value("${canineai.ai.colab-url}")
    private String colabUrl;

    @jakarta.annotation.PostConstruct
    public void init() {
        log.info("[AI] ToothSeg endpoint: {}", colabUrl);
    }

    @Override
    public void triggerInference(UUID jobId) {
        log.info("[Outbound Worker Pipeline] Registering Job ID: {} in QUEUED state for GPU Worker polling.", jobId);
        
        try {
            AIJob job = jobRepository.findById(jobId)
                    .orElseThrow(() -> new RuntimeException("Job not found in database: " + jobId));

            PredictionSource source = "demo".equalsIgnoreCase(aiMode) ? PredictionSource.DEMO : PredictionSource.REAL;
            
            // Mark job QUEUED so Outbound GPU Worker can pick it up via GET /api/v1/ai/worker/jobs/next
            job.setState(JobState.QUEUED);
            job.setProgressPercentage(10);
            job.setCurrentStage("QUEUED");
            job.setPredictionSource(source);
            jobRepository.save(job);
            
            log.info("[Outbound Worker Pipeline] Job ID: {} successfully QUEUED. Awaiting Colab GPU Worker claim.", jobId);
        } catch (Exception e) {
            log.error("[Outbound Worker Pipeline] Error registering job: {}", e.getMessage(), e);
        }
    }

    @Override
    public void cancelInference(UUID jobId) {
        Future<?> task = activeJobs.remove(jobId);
        if (task != null) {
            task.cancel(true);
            log.info("Cancelled running inference execution thread for job: {}", jobId);
        }
        jobRepository.findById(jobId).ifPresent(job -> {
            job.setState(JobState.CANCELLED);
            job.setCurrentStage("CANCELLED");
            jobRepository.save(job);
        });
    }

    private java.io.File resolveCbctFile(String storagePath) {
        if (storagePath != null && !storagePath.isBlank()) {
            java.util.List<java.io.File> candidates = java.util.List.of(
                new java.io.File(storagePath),
                new java.io.File("uploads", storagePath),
                new java.io.File("backend/uploads", storagePath),
                new java.io.File("../uploads", storagePath),
                new java.io.File("../backend/uploads", storagePath),
                new java.io.File("c:/Users/darsi/Downloads/CANINE_AI/uploads", storagePath)
            );
            for (java.io.File f : candidates) {
                if (f.exists() && f.isFile()) {
                    return f;
                }
                if (f.exists() && f.isDirectory()) {
                    java.io.File[] files = f.listFiles();
                    if (files != null) {
                        for (java.io.File file : files) {
                            String name = file.getName().toLowerCase();
                            if (name.endsWith(".nii") || name.endsWith(".nii.gz") || name.endsWith(".dcm") || name.endsWith(".zip")) {
                                return file;
                            }
                        }
                    }
                }
            }
        }

        // Search upload directories for any uploaded NIfTI file
        java.util.List<java.io.File> rootDirs = java.util.List.of(
            new java.io.File("uploads"),
            new java.io.File("backend/uploads"),
            new java.io.File("../uploads"),
            new java.io.File("c:/Users/darsi/Downloads/CANINE_AI/uploads")
        );
        for (java.io.File root : rootDirs) {
            if (root.exists() && root.isDirectory()) {
                java.io.File found = findNiftiRecursive(root);
                if (found != null) {
                    return found;
                }
            }
        }

        throw new IllegalStateException("Uploaded CBCT file not found for storagePath: " + storagePath);
    }

    private java.io.File findNiftiRecursive(java.io.File dir) {
        java.io.File[] files = dir.listFiles();
        if (files == null) return null;
        for (java.io.File f : files) {
            if (f.isFile()) {
                String name = f.getName().toLowerCase();
                if (name.endsWith(".nii") || name.endsWith(".nii.gz")) {
                    return f;
                }
            } else if (f.isDirectory()) {
                java.io.File found = findNiftiRecursive(f);
                if (found != null) return found;
            }
        }
        return null;
    }

    // callColabToothSegEndpoint removed since logic is now inline with polling
}
