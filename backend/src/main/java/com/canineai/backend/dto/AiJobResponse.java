package com.canineai.backend.dto;

import com.canineai.backend.entity.AiTaskType;
import com.canineai.backend.entity.JobState;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class AiJobResponse {
    private UUID id;
    private UUID studyId;
    private AiTaskType taskType;
    private JobState state;
    private String activeModelName;
    private String modelVersion;
    private int progressPercentage;
    private String resultJson;
    private String errorMessage;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
