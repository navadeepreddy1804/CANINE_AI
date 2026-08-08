package com.canineai.backend.service;

import com.canineai.backend.dto.UploadProgressResponse;
import com.canineai.backend.dto.UploadSessionResponse;
import java.io.InputStream;
import java.util.UUID;

public interface UploadService {

    /**
     * Initializes a chunked upload session for a specific Patient EMR.
     */
    UploadSessionResponse initializeSession(UUID patientId, long totalSize, int totalFiles, String username);

    /**
     * Streams an individual chunk part directly to storage.
     */
    void uploadChunk(UUID sessionId, String fileName, InputStream stream);

    /**
     * Unpacks and processes a ZIP stream of DICOM files on the fly.
     */
    void processZipStream(UUID sessionId, InputStream stream);

    /**
     * Retrieves session entity status.
     */
    UploadSessionResponse getSession(UUID sessionId);

    /**
     * Computes dynamically calculated progress speeds, elapsed and remaining times.
     */
    UploadProgressResponse getProgress(UUID sessionId);

    /**
     * Cancels active uploads.
     */
    void cancelSession(UUID sessionId);

    /**
     * Retries validation processing for a failed upload session.
     */
    void retrySession(UUID sessionId);
}
