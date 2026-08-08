package com.canineai.backend.dto;

import com.canineai.backend.entity.AiTaskType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

@Data
public class AiJobRequest {

    @NotNull(message = "Study ID is required")
    private UUID studyId;

    @NotNull(message = "Task type is required")
    private AiTaskType taskType;
}
