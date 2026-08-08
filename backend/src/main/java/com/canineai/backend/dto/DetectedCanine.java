package com.canineai.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DetectedCanine {
    private String fdi;
    private String side;
    private String arch;
    
    private BoundingBox boundingBox;
    private Double centroidX;
    private Double centroidY;
    private Double centroidZ;
    
    private String confidence;
    private String status;
    private String eruptionPrediction;
    
    private Double angulation;
    private Double depthMm;
    private Double distanceToOcclusalPlaneMm;
    private Double distanceToMidlineMm;
    private String overlapWithLateralIncisor;
    private Integer rootFormationPercentage;
    
    private String mesiodistalPosition;
    private String buccopalatalPosition;
    private String verticalPosition;
    private String eruptionPathAssessment;
}
