package com.canineai.backend.service;

import lombok.Builder;
import lombok.Data;
import java.io.InputStream;
import java.time.LocalDate;

public interface MetadataExtractor {

    /**
     * Extracts DICOM attributes from stream.
     */
    DicomMetadata extract(InputStream stream);

    @Data
    @Builder
    class DicomMetadata {
        private String patientName;
        private String patientId;
        private String studyInstanceUid;
        private String seriesInstanceUid;
        private String sopInstanceUid;
        private LocalDate studyDate;
        private String studyTime;
        private String manufacturer;
        private String deviceModel;
        private int sliceCount;
        private String voxelSize;
        private String pixelSpacing;
        private Double sliceThickness;
        private Integer rows;
        private Integer columns;
        private String modality;
        private String studyDescription;
        private String seriesDescription;
    }
}
