package com.canineai.backend.dto;

import com.canineai.backend.entity.JobState;
import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class AiProgressResponse {
    private UUID jobId;
    private JobState state;
    private int progressPercentage;
    private String currentStage; // e.g. Preprocessing, Segmentation
    private String currentModel;
    private long elapsedTimeSeconds;
    private long timeRemainingSeconds;
    private int gpuUsagePercent;
    private int cpuUsagePercent;
    private String errorMessage;
}
