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

    @Override
    public void triggerInference(UUID jobId) {
        log.info("Triggering async inference validation pipeline for Job ID: {}", jobId);

        Future<?> task = executorService.submit(() -> {
            try {
                int attempts = 0;
                java.util.Optional<AIJob> optionalJob = jobRepository.findById(jobId);
                while (optionalJob.isEmpty() && attempts < 15) {
                    Thread.sleep(200);
                    optionalJob = jobRepository.findById(jobId);
                    attempts++;
                }
                AIJob job = optionalJob.orElseThrow(() -> new RuntimeException("Job not found in database after waiting: " + jobId));

                PredictionSource source = "demo".equalsIgnoreCase(aiMode) ? PredictionSource.DEMO : PredictionSource.REAL;
                inferenceHelper.updateJobState(jobId, JobState.RUNNING, 10, "Preparing Study", null, null, source);

                ModelRegistry.ModelEndpoint endpoint = modelSelector.selectModel(job.getTaskType());

                com.canineai.backend.entity.Study study = studyRepository.findById(job.getStudyId())
                        .orElseThrow(() -> new IllegalStateException("Study not found"));
                String storagePath = studyStorageRepository.findByStudyId(job.getStudyId())
                        .map(com.canineai.backend.entity.StudyStorage::getStoragePath)
                        .orElse(null);

                log.info("[Spring] Forwarding to FastAPI for Study ID: {} (Endpoint: {})", job.getStudyId(), endpoint.getUrl());
                String acceptedJob = callFastApiEndpoint(endpoint, jobId, job.getStudyId(), study.getUploadSessionId(), storagePath)
                        .block();
                Map<String, Object> accepted = objectMapper.readValue(acceptedJob, new TypeReference<>() {});
                String externalJobId = String.valueOf(accepted.get("jobId"));
                if (externalJobId == null || externalJobId.isBlank() || "null".equals(externalJobId)) {
                    throw new IllegalStateException("FastAPI did not return an AI job identifier");
                }
                
                String result = pollFastApiJob(endpoint, externalJobId, jobId);

                AIJob activeJob = jobRepository.findById(jobId).orElse(null);
                if (activeJob != null && activeJob.getState() == JobState.CANCELLED) {
                    log.info("AI Analysis Job was cancelled by the user: {}", jobId);
                    return;
                }

                log.info("[Spring] Saving analysis report for Study ID: {}", job.getStudyId());
                inferenceHelper.updateJobState(jobId, JobState.COMPLETED, 100, "Completed", result, null, source);
                log.info("[Spring] Analysis successfully saved and completed for Job ID: {}", jobId);

            } catch (Exception e) {
                log.warn("ToothSeg external inference call failed or offline ({}), attempting self-contained fallback for Job: {}", e.getMessage(), jobId);
                AIJob activeJob = jobRepository.findById(jobId).orElse(null);
                if (activeJob != null && activeJob.getState() != JobState.CANCELLED) {
                    PredictionSource source = "demo".equalsIgnoreCase(aiMode) ? PredictionSource.DEMO : PredictionSource.REAL;
                    if ("demo".equalsIgnoreCase(aiMode) || e.getMessage() != null && e.getMessage().contains("offline")) {
                        try {
                            log.info("Running self-contained demo simulation for Study ID: {}", activeJob.getStudyId());
                            inferenceHelper.updateJobState(jobId, JobState.RUNNING, 50, "Simulating AI Segmentation", null, null, PredictionSource.DEMO);
                            Thread.sleep(500);
                            String demoJson = inferenceHelper.generateSelfContainedDemoJson(activeJob.getStudyId());
                            inferenceHelper.updateJobState(jobId, JobState.COMPLETED, 100, "Completed", demoJson, null, PredictionSource.DEMO);
                            log.info("Self-contained demo simulation completed successfully for Job: {}", jobId);
                            return;
                        } catch (Exception ex) {
                            log.error("Self-contained demo execution failed: {}", ex.getMessage(), ex);
                        }
                    }
                    inferenceHelper.updateJobState(jobId, JobState.FAILED, 0, "Failed", null, e.getMessage(), source);
                }
            } finally {
                activeJobs.remove(jobId);
            }
        });

        activeJobs.put(jobId, task);
    }

    @Override
    public void cancelInference(UUID jobId) {
        Future<?> task = activeJobs.remove(jobId);
        if (task != null) {
            task.cancel(true);
            log.info("Cancelled running inference execution thread for job: {}", jobId);
        }
    }

    private Mono<String> callFastApiEndpoint(ModelRegistry.ModelEndpoint endpoint, UUID jobId, UUID studyId, UUID sessionId, String storagePath) {
        log.info("Checking FastAPI health for endpoint: {}", endpoint.getUrl());
        String healthUrl = deriveHealthUrl(endpoint.getUrl());
        if (sessionId == null && (storagePath == null || storagePath.isBlank())) {
            return Mono.error(new IllegalStateException("Study has no source upload session or storage path"));
        }
        Map<String, String> payload = new java.util.HashMap<>();
        payload.put("jobId", jobId.toString());
        payload.put("studyId", studyId.toString());
        if (sessionId != null) {
            payload.put("sessionId", sessionId.toString());
        }
        if (storagePath != null && !storagePath.isBlank()) {
            payload.put("storagePath", storagePath);
        }

        return aiWebClient.get()
                .uri(healthUrl)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(5))
                .doOnNext(body -> log.info("FastAPI health response for {}: {}", healthUrl, body))
                .flatMap(healthBody -> {
                    log.info("FastAPI service healthy, invoking inference endpoint: {}", endpoint.getUrl());
                    return aiWebClient.post()
                        .uri(endpoint.getUrl())
                        .bodyValue(payload)
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(endpoint.getTimeoutSeconds()));
                })
                .retryWhen(Retry.fixedDelay(Math.min(endpoint.getRetryCount(), 1), Duration.ofSeconds(2))
                        .doBeforeRetry(retrySignal -> log.warn("Retrying FastAPI inference connection... Attempt: {}", retrySignal.totalRetries() + 1)))
                .onErrorResume(ex -> {
                    log.error("FastAPI inference service offline or unreachable at {}: {}", endpoint.getUrl(), ex.getMessage());
                    return Mono.error(new RuntimeException("AI inference service offline"));
                });
    }

    private String deriveHealthUrl(String endpointUrl) {
        if (endpointUrl == null || endpointUrl.isBlank()) {
            return "http://localhost:8002/api/v1/health";
        }
        if (endpointUrl.endsWith("/")) {
            endpointUrl = endpointUrl.substring(0, endpointUrl.length() - 1);
        }
        return endpointUrl.replaceAll("/[^/]+$", "/health");
    }

    private String pollFastApiJob(ModelRegistry.ModelEndpoint endpoint, String externalJobId, UUID jobId) throws Exception {
        String baseUrl = endpoint.getUrl().replaceAll("/inference$", "");
        long deadline = System.nanoTime() + Duration.ofSeconds(endpoint.getTimeoutSeconds()).toNanos();
        while (System.nanoTime() < deadline && !Thread.currentThread().isInterrupted()) {
            String payload = aiWebClient.get().uri(baseUrl + "/jobs/" + externalJobId)
                    .retrieve().bodyToMono(String.class).block(Duration.ofSeconds(10));
            Map<String, Object> status = objectMapper.readValue(payload, new TypeReference<>() {});
            int progress = ((Number) status.getOrDefault("progressPercentage", 0)).intValue();
            String state = String.valueOf(status.get("status"));
            
            PredictionSource source = "demo".equalsIgnoreCase(aiMode) ? PredictionSource.DEMO : PredictionSource.REAL;
            inferenceHelper.updateJobState(jobId, JobState.RUNNING, progress, null, null, null, source);
            
            if ("completed".equalsIgnoreCase(state)) {
                return objectMapper.writeValueAsString(status.get("result"));
            }
            if ("failed".equalsIgnoreCase(state) || "cancelled".equalsIgnoreCase(state)) {
                throw new IllegalStateException(String.valueOf(status.getOrDefault("errorMessage", "FastAPI job failed")));
            }
            Thread.sleep(750);
        }
        throw new java.util.concurrent.TimeoutException("FastAPI AI job timed out");
    }
}
