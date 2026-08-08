package com.canineai.backend.integration.workflow;

public enum WorkflowState {
    INITIATED,
    UPLOADING,
    UPLOADED,
    SEGMENTING,
    SEGMENTED,
    REPORT_GENERATING,
    REPORT_GENERATED,
    APPROVED,
    FAILED
}
