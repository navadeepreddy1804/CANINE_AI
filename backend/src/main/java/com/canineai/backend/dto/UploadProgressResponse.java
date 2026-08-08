package com.canineai.backend.dto;

import com.canineai.backend.entity.StudyStatus;
import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class UploadProgressResponse {
    private UUID sessionId;
    private StudyStatus status;
    private int progressPercentage;
    private int totalFiles;
    private int uploadedFiles;
    private long timeElapsedSeconds;
    private long timeRemainingSeconds;
    private long speedBytesPerSecond;
}
