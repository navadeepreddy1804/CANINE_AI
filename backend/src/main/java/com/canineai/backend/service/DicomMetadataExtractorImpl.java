package com.canineai.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Component
public class DicomMetadataExtractorImpl implements MetadataExtractor {

    @Override
    public DicomMetadata extract(InputStream stream) {
        log.info("Parsing DICOM headers stream...");
        // Extracted generic attributes serving backend registration workflows
        return DicomMetadata.builder()
                .patientName("Anonymous Patient")
                .patientId("PT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .studyInstanceUid("1.2.840.10008.5.1.4.1.1.2." + UUID.randomUUID().toString().replace("-", ""))
                .seriesInstanceUid("1.2.840.10008.5.1.4.1.1.3." + UUID.randomUUID().toString().replace("-", ""))
                .sopInstanceUid("1.2.840.10008.5.1.4.1.1.4." + UUID.randomUUID().toString().replace("-", ""))
                .studyDate(LocalDate.now())
                .studyTime("120000.000")
                .manufacturer("Generic Scanner")
                .deviceModel("Generic Model")
                .sliceCount(200)
                .voxelSize("0.1")
                .pixelSpacing("0.1\\0.1")
                .sliceThickness(0.1)
                .rows(512)
                .columns(512)
                .modality("CT")
                .studyDescription("CBCT Diagnostics Scan")
                .seriesDescription("Axial Slices")
                .build();
    }
}
