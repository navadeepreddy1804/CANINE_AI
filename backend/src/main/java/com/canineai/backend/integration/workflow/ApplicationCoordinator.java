package com.canineai.backend.integration.workflow;

import com.canineai.backend.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/integration/workflows")
@RequiredArgsConstructor
@Tag(name = "E2E Integration Coordinator", description = "Endpoints to orchestrate the end-to-end diagnostic workflow (DICOM Upload -> AI Segmentation -> LLM Report)")
public class ApplicationCoordinator {

    private final WorkflowManager workflowManager;

    @PostMapping("/trigger")
    @Operation(summary = "Trigger E2E Diagnostic Workflow", description = "Runs async ToothSeg segmentation on FastAPI, and passes structured coordinates directly to LLM generator.")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORTHODONTIST') or hasAuthority('reports:write')")
    public ResponseEntity<ApiResponse<WorkflowContext>> triggerWorkflow(
            @RequestParam("patientId") UUID patientId,
            @RequestParam("studyId") UUID studyId) {
        
        WorkflowContext context = workflowManager.startWorkflow(patientId, studyId);
        return ResponseEntity.ok(ApiResponse.success(context, "E2E Diagnostic integration workflow triggered successfully"));
    }

    @GetMapping("/status/{studyId}")
    @Operation(summary = "Check workflow pipeline state status", description = "Queries active state sync variables of the patient study journey.")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORTHODONTIST', 'CLINICIAN') or hasAuthority('reports:read')")
    public ResponseEntity<ApiResponse<WorkflowContext>> getWorkflowStatus(@PathVariable("studyId") UUID studyId) {
        WorkflowContext context = workflowManager.getWorkflowState(studyId);
        if (context == null) {
            return ResponseEntity.ok(ApiResponse.error("No active workflow traces found for Study: " + studyId));
        }
        return ResponseEntity.ok(ApiResponse.success(context, "Workflow status retrieved successfully"));
    }
}
