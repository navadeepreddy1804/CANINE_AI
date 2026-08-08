package com.canineai.backend.dto;

import lombok.Builder;
import lombok.Value;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Value
@Builder
public class PersistedReportDto {
    UUID reportId;
    String reportMarkdown;
    LocalDateTime reportCreatedAt;
    String doctorEmail;
    String patientName;
    String patientId;
    LocalDate patientDateOfBirth;
    String patientGender;
    UUID studyId;
    LocalDate studyDate;
    String modality;
    String studyDescription;
    Integer rows;
    Integer columns;
    String voxelSize;
    String pixelSpacing;
    Double sliceThickness;
    LocalDateTime analysisCompletedAt;
    String aiResultJson;
    List<Path> previewImagePaths;
}
