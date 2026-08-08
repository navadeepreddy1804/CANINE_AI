package com.canineai.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PredictionResult {
    private List<DetectedCanine> detectedCanines;
    private List<String> findings;
    
    private String prediction; // overallDiagnosis
    private String confidence; // overallConfidence
    private String clinicalRecommendation;
    
    // Kept for backward compatibility with Real ToothSeg (so Real mode doesn't break)
    private String canineToothName;
    private String canineFdi;
    private Double canineVolumeMm3;
    private Double angle;
    private String canineCentroid;
    private Integer toothCount;
    private Integer maxillaryTeethCount;
    private Integer mandibularTeethCount;
    private String sectorLocation;
    private String eruptionDirection;
    private String rootResorptionRisk;
    private String threatLevel;
    private String difficulty;
    private BoundingBox boundingBox;
}
