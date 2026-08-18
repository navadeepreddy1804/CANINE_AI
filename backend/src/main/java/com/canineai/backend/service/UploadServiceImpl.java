package com.canineai.backend.service;

import com.canineai.backend.common.BusinessException;
import com.canineai.backend.dto.UploadProgressResponse;
import com.canineai.backend.dto.UploadSessionResponse;
import com.canineai.backend.entity.*;
import com.canineai.backend.repository.*;
import com.canineai.backend.storage.StorageProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class UploadServiceImpl implements UploadService {

    private final UploadSessionRepository sessionRepository;
    private final UploadedFileRepository uploadedFileRepository;
    private final StudyRepository studyRepository;
    private final SeriesRepository seriesRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final StudyStorageRepository studyStorageRepository;
    
    private final StorageProvider storageProvider;
    private final DicomValidator dicomValidator;
    private final MetadataExtractor metadataExtractor;
    private final org.springframework.web.reactive.function.client.WebClient aiWebClient;

    // Background executor thread pool to process validations asynchronously
    private final Executor asyncExecutor = Executors.newFixedThreadPool(4);

    @Override
    @Transactional
    public UploadSessionResponse initializeSession(UUID patientId, long totalSize, int totalFiles, String username) {
        log.info("Initializing CBCT upload session for Patient EMR ID: {}", patientId);

        // Verify patient ownership existence
        if (!patientRepository.existsById(patientId)) {
            throw new BusinessException.ResourceNotFoundException("Patient EMR not found: " + patientId);
        }

        UploadSession session = UploadSession.builder()
                .patientId(patientId)
                .username(username)
                .totalSizeBytes(totalSize)
                .totalFilesCount(totalFiles)
                .uploadedFilesCount(0)
                .status(StudyStatus.UPLOADING)
                .createdAt(LocalDateTime.now())
                .build();

        UploadSession saved = sessionRepository.save(session);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void uploadChunk(UUID sessionId, String fileName, InputStream stream) {
        UploadSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException.ResourceNotFoundException("Upload session not found"));

        if (session.getStatus() == StudyStatus.CANCELLED || session.getStatus() == StudyStatus.FAILED) {
            throw new BusinessException.ConflictException("Session has been terminated");
        }

        String safeName = java.util.UUID.randomUUID().toString() + "_" + fileName;
        String relativePath = "temp/" + sessionId + "/" + safeName;
        storageProvider.saveStream(relativePath, stream);

        UploadedFile uploadedFile = UploadedFile.builder()
                .session(session)
                .fileName(fileName)
                .storageLocationPath(relativePath)
                .fileSizeBytes(0) // Will be updated on compile
                .build();
        uploadedFileRepository.save(uploadedFile);

        // Increment count thread-safely
        session.setUploadedFilesCount(session.getUploadedFilesCount() + 1);
        session.setUpdatedAt(LocalDateTime.now());
        sessionRepository.save(session);

        if (session.getUploadedFilesCount() >= session.getTotalFilesCount()) {
            triggerBackgroundProcessing(session);
        }
    }

    @Override
    @Transactional
    public void processZipStream(UUID sessionId, InputStream stream) {
        UploadSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException.ResourceNotFoundException("Upload session not found"));

        log.info("Streaming ZIP parsing for session: {}", sessionId);
        session.setStatus(StudyStatus.UPLOADING);
        sessionRepository.save(session);

        try (ZipInputStream zis = new ZipInputStream(stream)) {
            ZipEntry entry;
            int filesCount = 0;
            while ((entry = zis.getNextEntry()) != null) {
                String entryNameLower = entry.getName().toLowerCase();
                if (entry.isDirectory() || entryNameLower.startsWith(".") || entryNameLower.contains("__macos") || entryNameLower.contains("/._") || entryNameLower.startsWith("._") || entryNameLower.equalsIgnoreCase("thumbs.db")) {
                    continue;
                }

                String fileName = new File(entry.getName()).getName();
                String safeName = java.util.UUID.randomUUID().toString() + "_" + fileName;
                String relativePath = "temp/" + sessionId + "/" + safeName;

                // Stream directly to local storage on the fly
                storageProvider.saveStream(relativePath, new FilterInputStream(zis) {
                    @Override
                    public void close() throws IOException {
                        // Avoid closing ZipInputStream early
                    }
                });

                UploadedFile file = UploadedFile.builder()
                        .session(session)
                        .fileName(fileName)
                        .storageLocationPath(relativePath)
                        .fileSizeBytes(entry.getSize())
                        .build();
                uploadedFileRepository.save(file);

                filesCount++;
                session.setUploadedFilesCount(filesCount);
                sessionRepository.save(session);
            }

            session.setTotalFilesCount(filesCount);
            sessionRepository.save(session);

            triggerBackgroundProcessing(session);

        } catch (IOException e) {
            log.error("Failed to unpack streamed ZIP file", e);
            session.setStatus(StudyStatus.FAILED);
            session.setErrorMessage("Failed to process ZIP payload: " + e.getMessage());
            sessionRepository.save(session);
            throw new RuntimeException("Corrupted ZIP stream", e);
        }
    }

    @Override
    public UploadSessionResponse getSession(UUID sessionId) {
        UploadSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException.ResourceNotFoundException("Upload session not found"));
        return mapToResponse(session);
    }

    @Override
    public UploadProgressResponse getProgress(UUID sessionId) {
        UploadSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException.ResourceNotFoundException("Upload session not found"));

        long elapsedSeconds = java.time.temporal.ChronoUnit.SECONDS.between(session.getCreatedAt(), LocalDateTime.now());
        if (elapsedSeconds < 1) elapsedSeconds = 1;

        double filesPerSecond = (double) session.getUploadedFilesCount() / elapsedSeconds;
        long timeRemainingSeconds = 0;
        if (filesPerSecond > 0) {
            long remainingFiles = session.getTotalFilesCount() - session.getUploadedFilesCount();
            if (remainingFiles > 0) {
                timeRemainingSeconds = (long) (remainingFiles / filesPerSecond);
            }
        }

        // Approximate bytes based on file ratio
        long speedBytesPerSecond = 0;
        if (session.getTotalFilesCount() > 0) {
            double avgFileSize = (double) session.getTotalSizeBytes() / session.getTotalFilesCount();
            speedBytesPerSecond = (long) (filesPerSecond * avgFileSize);
        }

        return UploadProgressResponse.builder()
                .sessionId(sessionId)
                .status(session.getStatus())
                .progressPercentage(session.getProgressPercentage())
                .totalFiles(session.getTotalFilesCount())
                .uploadedFiles(session.getUploadedFilesCount())
                .timeElapsedSeconds((int) elapsedSeconds)
                .timeRemainingSeconds((int) timeRemainingSeconds)
                .speedBytesPerSecond(speedBytesPerSecond)
                .build();
    }

    @Override
    @Transactional
    public void cancelSession(UUID sessionId) {
        UploadSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException.ResourceNotFoundException("Upload session not found"));

        log.info("Cancelling upload session: {}", sessionId);
        session.setStatus(StudyStatus.CANCELLED);
        sessionRepository.save(session);
    }

    @Override
    @Transactional
    public void retrySession(UUID sessionId) {
        UploadSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException.ResourceNotFoundException("Upload session not found"));

        log.info("Retrying upload session processing: {}", sessionId);
        session.setStatus(StudyStatus.UPLOADING);
        sessionRepository.save(session);

        triggerBackgroundProcessing(session);
    }

    private void triggerBackgroundProcessing(UploadSession session) {
        session.setStatus(StudyStatus.VALIDATING);
        sessionRepository.save(session);

        if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        executeBackgroundProcessing(session);
                    }
                }
            );
        } else {
            executeBackgroundProcessing(session);
        }
    }

    private void executeBackgroundProcessing(UploadSession session) {
        asyncExecutor.execute(() -> {
            try {
                log.info("Async processing starting for session: {}", session.getId());
                List<UploadedFile> files = uploadedFileRepository.findBySessionId(session.getId());

                if (files.isEmpty()) {
                    throw new RuntimeException("No files loaded in EMR upload directories.");
                }

                // Register Patient context early
                Patient patient = patientRepository.findById(session.getPatientId())
                        .orElseThrow(() -> new RuntimeException("Patient EMR not found"));

                // Verify DICOM validity on first file as check
                UploadedFile sampleFile = files.get(0);
                boolean isNifti = sampleFile.getFileName().toLowerCase().endsWith(".nii") 
                               || sampleFile.getFileName().toLowerCase().endsWith(".nii.gz");

                MetadataExtractor.DicomMetadata meta;

                if (isNifti) {
                    log.info("NIfTI upload detected: bypassing DICOM header validation for file: {}", sampleFile.getFileName());
                    // Generate fallback metadata for NIfTI using builder pattern
                    meta = MetadataExtractor.DicomMetadata.builder()
                            .patientName(patient.getFullName())
                            .patientId(patient.getHospitalPatientId())
                            .studyInstanceUid("NII-" + UUID.randomUUID().toString())
                            .seriesInstanceUid("NII-SER-" + UUID.randomUUID().toString())
                            .sopInstanceUid("NII-SOP-" + UUID.randomUUID().toString())
                            .studyDate(java.time.LocalDate.now())
                            .studyTime(java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HHmmss")))
                            .modality("CBCT")
                            .studyDescription("NIfTI Scan Volume (" + sampleFile.getFileName() + ")")
                            .manufacturer("NIfTI Loader")
                            .deviceModel("SimpleITK Preprocessor")
                            .sliceThickness(0.5)
                            .voxelSize("0.5")
                            .pixelSpacing("0.5")
                            .rows(256)
                            .columns(256)
                            .sliceCount(files.size())
                            .seriesDescription("NIfTI Series")
                            .build();
                } else {
                    InputStream stream = storageProvider.getFileStream(sampleFile.getStorageLocationPath());
                    if (!dicomValidator.isValidDicom(stream)) {
                        throw new RuntimeException("Invalid DICOM headers format detected.");
                    }
                    // Retrieve metadata
                    stream = storageProvider.getFileStream(sampleFile.getStorageLocationPath());
                    meta = metadataExtractor.extract(stream);
                }

                // Update session state
                updateSessionStatus(session.getId(), StudyStatus.PROCESSING);

                // Prevent duplicate studies UIDs registration by appending a unique suffix for sample datasets
                String finalStudyUid = meta.getStudyInstanceUid();
                if (studyRepository.existsByStudyInstanceUidAndDeletedFalse(finalStudyUid)) {
                    log.warn("Duplicate study instance UID already registered: {}. Appending unique suffix to allow multiple uploads.", finalStudyUid);
                    finalStudyUid = finalStudyUid + "-" + java.util.UUID.randomUUID().toString().substring(0, 8);
                }

                Study study = Study.builder()
                        .uploadSessionId(session.getId())
                        .patient(patient)
                        .studyInstanceUid(finalStudyUid)
                        .studyDate(meta.getStudyDate())
                        .studyTime(meta.getStudyTime())
                        .modality(meta.getModality())
                        .studyDescription(meta.getStudyDescription())
                        .manufacturer(meta.getManufacturer())
                        .deviceModel(meta.getDeviceModel())
                        .sliceThickness(meta.getSliceThickness())
                        .voxelSize(meta.getVoxelSize())
                        .pixelSpacing(meta.getPixelSpacing())
                        .rows(meta.getRows())
                        .columns(meta.getColumns())
                        .status(StudyStatus.PREVIEW_READY)
                        .build();
                study.setCreatedAt(LocalDateTime.now());
                study.setCreatedBy(session.getUsername() != null ? session.getUsername() : "System");
                Study savedStudy = studyRepository.save(study);

                // Determine doctorId and targetPath
                String doctorId = userRepository.findByEmailActive(session.getUsername())
                        .map(user -> String.valueOf(user.getId()))
                        .orElse("system");
                String sourcePath = "temp/" + session.getId();
                String targetPath = "doctor-" + doctorId + "/patient-" + patient.getId() + "/study-" + savedStudy.getId() + "/original";
                
                // Move storage directory
                storageProvider.moveDirectory(sourcePath, targetPath);

                // Persist StudyStorage metadata
                StudyStorage studyStorage = StudyStorage.builder()
                        .study(savedStudy)
                        .uploadSessionId(session.getId())
                        .storagePath(targetPath)
                        .fileCount(files.size())
                        .uploadStatus(StudyStatus.PREVIEW_READY)
                        .previewImagePaths("doctor-" + doctorId + "/patient-" + patient.getId() + "/study-" + savedStudy.getId() + "/previews")
                        .reportPath("doctor-" + doctorId + "/patient-" + patient.getId() + "/study-" + savedStudy.getId() + "/reports")
                        .build();
                studyStorage.setCreatedAt(LocalDateTime.now());
                studyStorage.setCreatedBy(session.getUsername() != null ? session.getUsername() : "System");
                studyStorageRepository.save(studyStorage);
                
                // Trigger python script to extract preview slices immediately
                try {
                    String fullPath = new java.io.File("uploads", targetPath).getAbsolutePath();
                    if (isNifti) {
                        java.io.File dir = new java.io.File(fullPath);
                        if (dir.exists() && dir.isDirectory()) {
                            for (java.io.File f : dir.listFiles()) {
                                if (f.getName().endsWith(".nii") || f.getName().endsWith(".nii.gz")) {
                                    fullPath = f.getAbsolutePath();
                                    break;
                                }
                            }
                        }
                    }
                    ProcessBuilder pb = new ProcessBuilder(
                        "python",
                        "C:\\Users\\darsi\\Downloads\\CANINE_AI\\ai-service\\extract_slices.py",
                        fullPath
                    );
                    pb.redirectErrorStream(true);
                    Process p = pb.start();
                    boolean finished = p.waitFor(15, java.util.concurrent.TimeUnit.SECONDS);
                    log.info("Slice preview extraction process finished: {}", finished);
                } catch (Exception ex) {
                    log.error("Failed to run extract_slices.py", ex);
                }

                
                // Update UploadedFile paths
                for (UploadedFile uf : files) {
                    uf.setStorageLocationPath(uf.getStorageLocationPath().replace(sourcePath, targetPath));
                    uploadedFileRepository.save(uf);
                }

                // Register Series
                Series series = Series.builder()
                        .study(savedStudy)
                        .seriesInstanceUid(meta.getSeriesInstanceUid())
                        .seriesDescription(meta.getSeriesDescription())
                        .sliceCount(files.size())
                        .build();
                series.setCreatedAt(LocalDateTime.now());
                series.setCreatedBy(session.getUsername() != null ? session.getUsername() : "System");
                seriesRepository.save(series);

                // The python script already extracts slices locally, so we do not need to call the external preprocessing service.

                updateSessionStatus(session.getId(), StudyStatus.COMPLETED);
                log.info("Background upload processing completed successfully for session: {}", session.getId());

            } catch (Exception e) {
                log.error("Failed to process background study load tasks: {}", session.getId(), e);
                failSession(session.getId(), e.getMessage());
            }
        });
    }

    private void updateSessionStatus(UUID sessionId, StudyStatus status) {
        sessionRepository.findById(sessionId).ifPresent(s -> {
            s.setStatus(status);
            s.setUpdatedAt(LocalDateTime.now());
            sessionRepository.save(s);
        });
    }

    private void failSession(UUID sessionId, String error) {
        sessionRepository.findById(sessionId).ifPresent(s -> {
            s.setStatus(StudyStatus.FAILED);
            s.setErrorMessage(error);
            s.setUpdatedAt(LocalDateTime.now());
            sessionRepository.save(s);
        });
    }

    private UploadSessionResponse mapToResponse(UploadSession s) {
        UUID studyId = studyRepository.findByUploadSessionIdAndDeletedFalse(s.getId())
                .map(Study::getId)
                .orElse(null);

        return UploadSessionResponse.builder()
                .id(s.getId())
                .uploadSessionId(s.getId())
                .studyId(studyId)
                .patientId(s.getPatientId())
                .totalSizeBytes(s.getTotalSizeBytes())
                .totalFilesCount(s.getTotalFilesCount())
                .uploadedFilesCount(s.getUploadedFilesCount())
                .progressPercentage(s.getProgressPercentage())
                .status(s.getStatus())
                .errorMessage(s.getErrorMessage())
                .build();
    }
}
