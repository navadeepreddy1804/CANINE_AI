package com.canineai.backend.dto;

import com.canineai.backend.entity.ReportStatus;
import com.canineai.backend.entity.ReportStyle;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@Schema(description = "Structured clinical markdown report details")
public class ReportResponse {

    @Schema(description = "Report identifier", example = "d94bfa20-410a-429a-8b1c-c760cdcae104")
    private UUID id;

    @Schema(description = "CBCT scan study identifier link", example = "a1b2c3d4-e5f6-7a8b-9c0d-e1f2a3b4c5d6")
    private UUID studyId;

    @Schema(description = "Report status state", example = "DRAFT")
    private ReportStatus status;

    @Schema(description = "Report layout style selection", example = "CLINICAL")
    private ReportStyle reportStyle;

    @Schema(description = "Raw markdown generated report contents")
    private String reportMarkdown;

    @Schema(description = "Persisted report template source", example = "rule-based-template")
    private String activeProvider;

    @Schema(description = "Prompt template key", example = "ortho-summary")
    private String promptTemplateKey;

    @Schema(description = "Report template version", example = "v1.2.0")
    private String templateVersion;

    @Schema(description = "Generation latency duration in milliseconds", example = "850")
    private Long generationLatencyMs;

    @Schema(description = "Orthodontist custom inputs feedback", example = "Emphasize impacted Canine #13 alignment vectors")
    private String doctorComments;

    @Schema(description = "Clinician signing authority email", example = "dr.darshan@metrodiagnostics.com")
    private String approvedBy;

    @Schema(description = "Signing datetime timestamp", example = "2026-07-12T10:15:30")
    private LocalDateTime approvedAt;

    @Schema(description = "Patient ID")
    private UUID patientId;

    @Schema(description = "Patient full name")
    private String patientName;

    @Schema(description = "Hospital Patient Display ID")
    private String patientDisplayId;

    @Schema(description = "CBCT scan study date")
    private String studyDate;

    @Schema(description = "Identified diagnostic prediction")
    private String prediction;

    @Schema(description = "Confidence percentage of diagnosis")
    private String confidence;

    @Schema(description = "Surgical / orthodontic difficulty level")
    private String difficulty;

    @Schema(description = "Root resorption risk rating")
    private String rootResorptionRisk;

    @Schema(description = "Clinical treatment recommendation details")
    private String clinicalRecommendation;

    @Schema(description = "Raw AI inference JSON output containing ToothSeg anatomical bounding box and measurements")
    private String aiResultJson;

    @Schema(description = "Canine tooth name", example = "Maxillary Right Canine")
    private String canineToothName;

    @Schema(description = "FDI tooth label", example = "13")
    private String canineFdi;

    @Schema(description = "Quadrant / sector", example = "Sector 1 (Right)")
    private String canineSector;

    @Schema(description = "Canine volume in mm3", example = "440.5")
    private Double canineVolumeMm3;

    @Schema(description = "Canine 3D PCA angulation degrees", example = "32.4")
    private Double canineAngulation;

    @Schema(description = "Canine 3D centroid coordinates", example = "[256.0, 180.2, 120.5]")
    private String canineCentroid;

    @Schema(description = "Total segmented teeth count", example = "30")
    private Integer totalTeethCount;

    @Schema(description = "Maxillary segmented teeth count", example = "14")
    private Integer maxillaryTeethCount;

    @Schema(description = "Mandibular segmented teeth count", example = "16")
    private Integer mandibularTeethCount;

    @Schema(description = "Canine bounding box slice index", example = "6")
    private Integer boundingBoxSliceIndex;

    @Schema(description = "Canine bounding box X coordinate", example = "180.0")
    private Double boundingBoxX;

    @Schema(description = "Canine bounding box Y coordinate", example = "160.0")
    private Double boundingBoxY;

    @Schema(description = "Canine bounding box width", example = "140.0")
    private Double boundingBoxWidth;

    @Schema(description = "Canine bounding box height", example = "140.0")
    private Double boundingBoxHeight;

    @Schema(description = "Human-friendly Study Display ID", example = "ST-20260808-0001")
    private String studyDisplayId;

    @Schema(description = "Minimum configured confidence threshold percentage", example = "63")
    private Integer minConfidenceThreshold;

    @Schema(description = "Clinical interpretation of confidence score", example = "Within acceptable demo threshold")
    private String confidenceInterpretation;

    @Schema(description = "AI-generated clinical decision support suggestions")
    private String clinicalSuggestions;

    @Schema(description = "Formatted approved/generated timestamp string", example = "08 Aug 2026, 08:32 AM")
    private String formattedApprovedAt;
}
