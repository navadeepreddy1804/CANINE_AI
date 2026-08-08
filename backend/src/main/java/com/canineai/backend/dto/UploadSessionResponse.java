package com.canineai.backend.dto;

import com.canineai.backend.entity.StudyStatus;
import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class UploadSessionResponse {
    /** Existing response identifier retained for clients already using {@code id}. */
    private UUID id;
    /** Explicit name for the same upload session identifier for new clients. */
    private UUID uploadSessionId;
    /**
     * Persisted study created from this upload. It is null while validation and
     * preprocessing are still running, and populated only after the Study row
     * exists in the database.
     */
    private UUID studyId;
    private UUID patientId;
    private long totalSizeBytes;
    private int totalFilesCount;
    private int uploadedFilesCount;
    private int progressPercentage;
    private StudyStatus status;
    private String errorMessage;
}
