package com.canineai.backend.controller;

import com.canineai.backend.common.ApiResponse;
import com.canineai.backend.dto.AiJobRequest;
import com.canineai.backend.dto.AiJobResponse;
import com.canineai.backend.dto.AiProgressResponse;
import com.canineai.backend.service.AiHealthService;
import com.canineai.backend.service.AiJobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/ai/jobs")
@RequiredArgsConstructor
@Tag(name = "AI Diagnostics Gateway", description = "Endpoints for triggering ToothSeg and monitoring background task pipelines")
public class AiController {

    private final AiJobService aiJobService;
    private final AiHealthService aiHealthService;

    @PostMapping
    @Operation(summary = "Submit AI analysis job", description = "Creates a database job transaction and triggers async WebClient FastAPI requests.")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORTHODONTIST') or hasAuthority('ai:write')")
    public ResponseEntity<ApiResponse<AiJobResponse>> submitJob(
            @Valid @RequestBody AiJobRequest request,
            Principal principal) {
        
        log.info("[Spring] POST /ai/jobs received for studyId={}", request.getStudyId());
        String currentUser = principal != null ? principal.getName() : "System";
        AiJobResponse response = aiJobService.submitJob(request, currentUser);
        log.info("[Spring] AI Job registered with ID: {} for Study ID: {}", response.getId(), request.getStudyId());
        return ResponseEntity.ok(ApiResponse.success(response, "AI Job submitted successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fetch AI Job details log", description = "Retrieves execution timestamps, configurations, error logs and results.")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORTHODONTIST', 'CLINICIAN') or hasAuthority('ai:read')")
    public ResponseEntity<ApiResponse<AiJobResponse>> getJob(@PathVariable("id") UUID id) {
        AiJobResponse response = aiJobService.getJob(id);
        return ResponseEntity.ok(ApiResponse.success(response, "AI Job details retrieved successfully"));
    }

    @GetMapping("/{id}/status")
    @Operation(summary = "Get dynamic progress updates", description = "Fetches progress stages, percentages, remaining times, and GPU statuses.")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORTHODONTIST', 'CLINICIAN') or hasAuthority('ai:read')")
    public ResponseEntity<ApiResponse<AiProgressResponse>> getProgress(@PathVariable("id") UUID id) {
        AiProgressResponse response = aiJobService.getProgress(id);
        return ResponseEntity.ok(ApiResponse.success(response, "AI Job progress retrieved successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete AI Job record log", description = "Soft deletes the job entry trace from EMR databases.")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ai:delete')")
    public ResponseEntity<ApiResponse<Void>> deleteJob(
            @PathVariable("id") UUID id,
            Principal principal) {
        
        String currentUser = principal != null ? principal.getName() : "System";
        aiJobService.deleteJob(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(null, "AI Job log deleted successfully"));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel running inference query", description = "Halts executor task hooks and marks the state machine CANCELLED.")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORTHODONTIST') or hasAuthority('ai:write')")
    public ResponseEntity<ApiResponse<Void>> cancelJob(
            @PathVariable("id") UUID id,
            Principal principal) {
        
        String currentUser = principal != null ? principal.getName() : "System";
        aiJobService.cancelJob(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(null, "AI Job cancelled successfully"));
    }

    @GetMapping("/metrics")
    @Operation(summary = "Fetch Gateway monitoring metrics", description = "Query running tasks queue, average latency durations, and GPU hardware states.")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ai:read')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getGatewayMetrics() {
        Map<String, Object> response = aiHealthService.getGatewayMetrics();
        return ResponseEntity.ok(ApiResponse.success(response, "AI Gateway metrics retrieved successfully"));
    }
}
