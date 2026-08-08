package com.canineai.backend.service;

import java.util.UUID;

public interface InferenceService {

    /**
     * Executes non-blocking API requests to external FastAPI services.
     */
    void triggerInference(UUID jobId);

    /**
     * Clears active connection hooks to terminate running tasks.
     */
    void cancelInference(UUID jobId);
}
