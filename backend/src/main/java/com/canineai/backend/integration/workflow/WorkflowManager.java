package com.canineai.backend.integration.workflow;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowManager {

    private final WorkflowEngine workflowEngine;
    private final Map<UUID, WorkflowContext> activeWorkflows = new ConcurrentHashMap<>();

    /**
     * Initializes and triggers the end-to-end diagnostic workflow run.
     */
    public WorkflowContext startWorkflow(UUID patientId, UUID studyId) {
        log.info("Request to initialize E2E Workflow. Patient: {}, Study: {}", patientId, studyId);

        WorkflowContext context = WorkflowContext.builder()
                .patientId(patientId)
                .studyId(studyId)
                .state(WorkflowState.INITIATED)
                .build();

        activeWorkflows.put(studyId, context);
        workflowEngine.executeWorkflow(context);
        return context;
    }

    public WorkflowContext getWorkflowState(UUID studyId) {
        return activeWorkflows.get(studyId);
    }
}
