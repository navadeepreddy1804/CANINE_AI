package com.canineai.backend.integration.workflow;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class WorkflowContext {
    private UUID patientId;
    private UUID studyId;
    private UUID jobId;
    private UUID reportId;
    private WorkflowState state;
    private int aiRetryCount;
    private int llmRetryCount;
    private String activeProvider;
    private String errorMessage;
}
