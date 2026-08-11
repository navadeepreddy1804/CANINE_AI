package com.canineai.backend.controller;

import com.canineai.backend.common.ApiResponse;
import com.canineai.backend.dto.UploadProgressResponse;
import com.canineai.backend.dto.UploadSessionResponse;
import com.canineai.backend.service.UploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/uploads")
@RequiredArgsConstructor
@Tag(name = "CBCT DICOM Uploads", description = "Endpoints for initializing sessions, streaming chunks, and unpacking ZIP files")
public class UploadController {

    private final UploadService uploadService;
    private final com.canineai.backend.service.PatientService patientService;

    private UUID resolvePatientId(String patientIdStr, String username) {
        if (patientIdStr == null || patientIdStr.isBlank()) {
            throw new IllegalArgumentException("patientId parameter is required");
        }
        try {
            return UUID.fromString(patientIdStr);
        } catch (IllegalArgumentException e) {
            return patientService.getPatientByHospitalId(patientIdStr, username).getId();
        }
    }

    @PostMapping
    @Operation(summary = "Initialize chunked upload session", description = "Verifies patient EMR and allocates a new upload session.")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORTHODONTIST') or hasAuthority('upload:create')")
    public ResponseEntity<ApiResponse<UploadSessionResponse>> initializeSession(
            @RequestParam("patientId") String patientIdStr,
            @RequestParam("totalSize") long totalSize,
            @RequestParam("totalFiles") int totalFiles,
            Principal principal) {
        
        String username = principal != null ? principal.getName() : "System";
        UUID patientId = resolvePatientId(patientIdStr, username);
        UploadSessionResponse response = uploadService.initializeSession(patientId, totalSize, totalFiles, username);
        return ResponseEntity.ok(ApiResponse.success(response, "Upload session initialized successfully"));
    }

    @PostMapping(value = "/{id}/chunk", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Operation(summary = "Stream upload individual chunk slice", description = "Saves slice chunks directly to local folders without bloating memory.")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORTHODONTIST') or hasAuthority('upload:write')")
    public ResponseEntity<ApiResponse<Void>> uploadChunk(
            @PathVariable("id") UUID id,
            @RequestParam("fileName") String fileName,
            HttpServletRequest request) {
        
        try (InputStream is = request.getInputStream()) {
            uploadService.uploadChunk(id, fileName, is);
            return ResponseEntity.ok(ApiResponse.success(null, "Chunk file saved successfully"));
        } catch (IOException e) {
            log.error("Failed to read chunk input stream for session: {}", id, e);
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to write chunk: " + e.getMessage()));
        }
    }

    @PostMapping(value = "/zip", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Operation(summary = "Stream upload ZIP DICOM package on-the-fly", description = "Streams and unpacks ZIP entries sequentially directly to disk storage.")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORTHODONTIST') or hasAuthority('upload:write')")
    public ResponseEntity<ApiResponse<UploadSessionResponse>> uploadZip(
            @RequestParam("patientId") String patientIdStr,
            HttpServletRequest request,
            Principal principal) {
        
        String username = principal != null ? principal.getName() : "System";
        UUID patientId = resolvePatientId(patientIdStr, username);
        log.info("Initializing streaming ZIP unpack session for patient: {}", patientId);

        // Initialize session with mock sizing parameters, which will be updated on ZIP parsing completion
        UploadSessionResponse session = uploadService.initializeSession(patientId, request.getContentLengthLong(), 1, username);
        
        try (InputStream is = request.getInputStream()) {
            uploadService.processZipStream(session.getId(), is);
            UploadSessionResponse finalStatus = uploadService.getSession(session.getId());
            return ResponseEntity.ok(ApiResponse.success(finalStatus, "ZIP uploaded and background unpacking pipeline triggered successfully"));
        } catch (IOException e) {
            log.error("Failed to parse streaming ZIP upload body for patient: {}", patientId, e);
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to write ZIP stream: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fetch session details status", description = "Retrieves current status metrics of a CBCT transfer session.")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORTHODONTIST', 'CLINICIAN') or hasAuthority('upload:read')")
    public ResponseEntity<ApiResponse<UploadSessionResponse>> getSession(@PathVariable("id") UUID id) {
        UploadSessionResponse response = uploadService.getSession(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Session details retrieved successfully"));
    }

    @GetMapping("/progress/{id}")
    @Operation(summary = "Get dynamic progress speeds", description = "Calculates elapsed timing durations and speed megabytes estimates.")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORTHODONTIST', 'CLINICIAN') or hasAuthority('upload:read')")
    public ResponseEntity<ApiResponse<UploadProgressResponse>> getProgress(@PathVariable("id") UUID id) {
        UploadProgressResponse response = uploadService.getProgress(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Upload progress metrics retrieved successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Terminate upload session", description = "Deletes active session references.")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('upload:delete')")
    public ResponseEntity<ApiResponse<Void>> deleteSession(@PathVariable("id") UUID id) {
        uploadService.cancelSession(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Upload session deleted successfully"));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel active transfer", description = "Instructs backend worker loops to discard active upload chunks.")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORTHODONTIST') or hasAuthority('upload:write')")
    public ResponseEntity<ApiResponse<Void>> cancelSession(@PathVariable("id") UUID id) {
        uploadService.cancelSession(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Upload cancelled successfully"));
    }

    @PostMapping("/{id}/retry")
    @Operation(summary = "Retry failed validation task", description = "Reruns metadata extractor triggers on previously failed uploads.")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORTHODONTIST') or hasAuthority('upload:write')")
    public ResponseEntity<ApiResponse<Void>> retrySession(@PathVariable("id") UUID id) {
        uploadService.retrySession(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Upload retry pipeline triggered successfully"));
    }
}
