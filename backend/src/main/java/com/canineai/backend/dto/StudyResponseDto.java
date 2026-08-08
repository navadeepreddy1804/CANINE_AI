package com.canineai.backend.dto;

import com.canineai.backend.entity.StudyStatus;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class StudyResponseDto {
    private UUID id;
    private UUID patientId;
    private String studyInstanceUid;
    private LocalDate studyDate;
    private String studyTime;
    private String modality;
    private String studyDescription;
    private String manufacturer;
    private String deviceModel;
    private String voxelSize;
    private String pixelSpacing;
    private Double sliceThickness;
    private Integer rows;
    private Integer columns;
    private StudyStatus status;
    private LocalDateTime createdAt;
}
