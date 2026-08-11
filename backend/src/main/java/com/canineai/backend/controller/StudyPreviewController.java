package com.canineai.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.canineai.backend.entity.StudyStorage;
import com.canineai.backend.repository.StudyStorageRepository;

@Slf4j
@RestController
@RequestMapping("/studies")
@Tag(name = "Study Previews", description = "Endpoints for retrieving representative axial, coronal, and sagittal slice images")
public class StudyPreviewController {

    private final List<Path> uploadRoots;
    private final StudyStorageRepository studyStorageRepository;

    // 1x1 transparent PNG fallback bytes
    private static final byte[] PLACEHOLDER_PNG = new byte[]{
        (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
        0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, 0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4,
        (byte) 0x89, 0x00, 0x00, 0x00, 0x0D, 0x49, 0x44, 0x41, 0x54, 0x78, (byte) 0xDA, 0x63, 0x60, 0x60, 0x60,
        0x00, 0x00, 0x00, 0x02, 0x00, 0x01, 0x73, 0x75, 0x01, 0x18, 0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44,
        (byte) 0xAE, 0x42, 0x60, (byte) 0x82
    };

    public StudyPreviewController(
            @Value("${canineai.storage.upload-dir:uploads}") String uploadDir,
            StudyStorageRepository studyStorageRepository) {
        this.uploadRoots = resolveUploadRoots(uploadDir);
        this.studyStorageRepository = studyStorageRepository;
    }

    private List<Path> resolveUploadRoots(String configuredUploadDir) {
        List<Path> roots = new ArrayList<>();
        Path workingDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();

        if (configuredUploadDir != null && !configuredUploadDir.isBlank()) {
            Path configuredPath = Paths.get(configuredUploadDir);
            if (!configuredPath.isAbsolute()) {
                configuredPath = workingDir.resolve(configuredUploadDir).normalize();
            }
            roots.add(configuredPath);
        }

        roots.add(workingDir.resolve("uploads").normalize());
        roots.add(workingDir.resolve("backend").resolve("uploads").normalize());

        Path parentDir = workingDir.getParent();
        if (parentDir != null) {
            roots.add(parentDir.resolve("uploads").normalize());
            roots.add(parentDir.resolve("backend").resolve("uploads").normalize());
        }

        return roots.stream().distinct().toList();
    }

    @GetMapping(value = "/{id}/previews/{type}", produces = MediaType.IMAGE_PNG_VALUE)
    @Operation(summary = "Get study slice preview image", description = "Serves the extracted PNG slice image (axial, coronal, or sagittal).")
    public ResponseEntity<byte[]> getPreviewImage(
            @PathVariable("id") UUID id,
            @PathVariable("type") String type) {
            
        // Sanitize type parameter
        String cleanType = type.toLowerCase().replaceAll("[^a-z]", "");
        if (!"axial".equals(cleanType) && !"coronal".equals(cleanType) && !"sagittal".equals(cleanType)) {
            return ResponseEntity.badRequest().build();
        }

        try {
            Optional<Path> previewPath = resolvePreviewPath(id, cleanType, null);
            if (previewPath.isPresent()) {
                Path filePath = previewPath.get();
                byte[] bytes = Files.readAllBytes(filePath);
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_PNG)
                        .body(bytes);
            }
        } catch (Exception e) {
            log.error("Failed to read study preview slice image: id={}, type={}", id, cleanType, e);
        }

        return ResponseEntity.notFound().build();
    }

    @GetMapping(value = "/{id}/previews/{type}/{index}", produces = MediaType.IMAGE_PNG_VALUE)
    @Operation(summary = "Get indexed study slice preview image", description = "Serves the extracted PNG slice image by index.")
    public ResponseEntity<byte[]> getIndexedPreviewImage(
            @PathVariable("id") UUID id,
            @PathVariable("type") String type,
            @PathVariable("index") int index) {
            
        String cleanType = type.toLowerCase().replaceAll("[^a-z]", "");
        if (!"axial".equals(cleanType) && !"coronal".equals(cleanType) && !"sagittal".equals(cleanType)) {
            return ResponseEntity.badRequest().build();
        }

        try {
            Optional<Path> previewPath = resolvePreviewPath(id, cleanType, index);
            if (previewPath.isPresent()) {
                Path filePath = previewPath.get();
                byte[] bytes = Files.readAllBytes(filePath);
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_PNG)
                        .body(bytes);
            }
        } catch (Exception e) {
            log.error("Failed to read study indexed preview slice image: id={}, type={}, index={}", id, cleanType, index, e);
        }

        return ResponseEntity.notFound().build();
    }

    @GetMapping(value = "/{id}/dicom/list", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List DICOM files", description = "Returns a list of DICOM file names associated with the study.")
    public ResponseEntity<List<String>> listDicomFiles(@PathVariable("id") UUID id) {
        Optional<StudyStorage> storageOpt = studyStorageRepository.findByStudyId(id);
        if (storageOpt.isEmpty() || storageOpt.get().getStoragePath() == null) {
            return ResponseEntity.notFound().build();
        }
        
        String storagePathStr = storageOpt.get().getStoragePath();
        List<String> dicomFiles = new ArrayList<>();
        
        for (Path uploadRoot : this.uploadRoots) {
            Path dicomDir = uploadRoot.resolve(storagePathStr).normalize();
            if (Files.exists(dicomDir) && Files.isDirectory(dicomDir)) {
                try (java.util.stream.Stream<Path> stream = Files.list(dicomDir)) {
                    stream.filter(Files::isRegularFile)
                          .map(Path::getFileName)
                          .map(Path::toString)
                          .filter(name -> name.toLowerCase().endsWith(".dcm") || name.toLowerCase().endsWith(".ima"))
                          .sorted()
                          .forEach(dicomFiles::add);
                    if (!dicomFiles.isEmpty()) {
                        break;
                    }
                } catch (Exception e) {
                    log.error("Failed to list DICOM files for study {}", id, e);
                }
            }
        }
        
        return ResponseEntity.ok(dicomFiles);
    }

    @GetMapping(value = "/{id}/dicom/{filename}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Operation(summary = "Download DICOM file", description = "Downloads a specific DICOM slice file.")
    public ResponseEntity<byte[]> getDicomFile(
            @PathVariable("id") UUID id,
            @PathVariable("filename") String filename) {
        
        // Prevent path traversal
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return ResponseEntity.badRequest().build();
        }
        
        Optional<StudyStorage> storageOpt = studyStorageRepository.findByStudyId(id);
        if (storageOpt.isEmpty() || storageOpt.get().getStoragePath() == null) {
            return ResponseEntity.notFound().build();
        }
        
        String storagePathStr = storageOpt.get().getStoragePath();
        
        for (Path uploadRoot : this.uploadRoots) {
            Path dicomFile = uploadRoot.resolve(storagePathStr).resolve(filename).normalize();
            if (isValidPath(dicomFile, uploadRoot) && Files.exists(dicomFile) && Files.isRegularFile(dicomFile)) {
                try {
                    return ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                            .contentType(MediaType.APPLICATION_OCTET_STREAM)
                            .body(Files.readAllBytes(dicomFile));
                } catch (Exception e) {
                    log.error("Failed to read DICOM file {} for study {}", filename, id, e);
                }
            }
        }
        
        return ResponseEntity.notFound().build();
    }

    // Removed procedural image generation fallbacks


    private Optional<Path> resolvePreviewPath(UUID id, String cleanType, Integer index) {
        Optional<StudyStorage> storage = studyStorageRepository.findByStudyId(id);
        if (storage.isEmpty() || storage.get().getPreviewImagePaths() == null) {
            return Optional.empty();
        }
        String previewPathStr = storage.get().getPreviewImagePaths();

        for (Path uploadRoot : this.uploadRoots) {
            Path previewRoot = uploadRoot.resolve(previewPathStr).normalize();
            if (index != null) {
                List<Path> candidates = List.of(
                    previewRoot.resolve(cleanType + "_" + index + ".png").normalize(),
                    previewRoot.resolve(cleanType).resolve(cleanType + "_" + index + ".png").normalize(),
                    previewRoot.resolve(cleanType).resolve(index + ".png").normalize(),
                    previewRoot.resolve(cleanType + "_overlay_" + index + ".png").normalize(),
                    previewRoot.resolve(cleanType).resolve(cleanType + "_overlay_" + index + ".png").normalize()
                );
                for (Path candidate : candidates) {
                    if (isValidPath(candidate, uploadRoot) && Files.exists(candidate) && Files.isRegularFile(candidate)) {
                        return Optional.of(candidate);
                    }
                }
                continue;
            }

            List<Path> directCandidates = List.of(
                previewRoot.resolve(cleanType + ".png").normalize(),
                previewRoot.resolve(cleanType).resolve(cleanType + ".png").normalize(),
                previewRoot.resolve(cleanType).resolve("middle.png").normalize()
            );
            for (Path candidate : directCandidates) {
                if (isValidPath(candidate, uploadRoot) && Files.exists(candidate) && Files.isRegularFile(candidate)) {
                    return Optional.of(candidate);
                }
            }

            for (int fallbackIndex = 0; fallbackIndex < 20; fallbackIndex++) {
                List<Path> fallbacks = List.of(
                    previewRoot.resolve(cleanType + "_" + fallbackIndex + ".png").normalize(),
                    previewRoot.resolve(cleanType).resolve(cleanType + "_" + fallbackIndex + ".png").normalize(),
                    previewRoot.resolve(cleanType).resolve(fallbackIndex + ".png").normalize()
                );
                for (Path candidate : fallbacks) {
                    if (isValidPath(candidate, uploadRoot) && Files.exists(candidate) && Files.isRegularFile(candidate)) {
                        return Optional.of(candidate);
                    }
                }
            }
        }
        return Optional.empty();
    }

    private boolean isValidPath(Path path, Path uploadRoot) {
        String pathStr = path.toAbsolutePath().toString().toLowerCase().replace('\\', '/');
        String rootStr = uploadRoot.toAbsolutePath().toString().toLowerCase().replace('\\', '/');
        return pathStr.startsWith(rootStr);
    }
}

