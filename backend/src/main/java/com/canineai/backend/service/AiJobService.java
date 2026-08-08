package com.canineai.backend.service;

import com.canineai.backend.dto.AiJobRequest;
import com.canineai.backend.dto.AiJobResponse;
import com.canineai.backend.dto.AiProgressResponse;
import java.util.UUID;

public interface AiJobService {

    /**
     * Initializes and registers an AI Job record.
     */
    AiJobResponse submitJob(AiJobRequest request, String currentUser);

    /**
     * Retrieves status response logs.
     */
    AiJobResponse getJob(UUID jobId);

    /**
     * Computes progress metrics (stage, estimated time, speeds).
     */
    AiProgressResponse getProgress(UUID jobId);

    /**
     * Instructs backend loops to cancel active inference queries.
     */
    void cancelJob(UUID jobId, String currentUser);

    /**
     * Deletes job logs.
     */
    void deleteJob(UUID jobId, String currentUser);
}
