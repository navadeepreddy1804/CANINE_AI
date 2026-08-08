package com.canineai.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DemoPredictionEngine {

    private final ObjectMapper objectMapper;

    public String generatePredictionJson(UUID studyId) {
        long seed = studyId.getMostSignificantBits() ^ studyId.getLeastSignificantBits();
        Random rng = new Random(seed);

        boolean impacted = rng.nextDouble() < 0.62;
        double angle = Math.round((impacted ? (27.0 + rng.nextDouble() * 31.0) : (5.0 + rng.nextDouble() * 19.0)) * 10.0) / 10.0;
        double confidence = Math.round((0.79 + rng.nextDouble() * 0.18) * 1000.0) / 1000.0;
        double eruptionProbability = Math.round((impacted ? (0.12 + rng.nextDouble() * 0.36) : (0.72 + rng.nextDouble() * 0.24)) * 1000.0) / 1000.0;

        String[] risks = {"LOW", "MODERATE", "HIGH"};
        String risk = impacted ? risks[rng.nextInt(3)] : (rng.nextDouble() < 0.8 ? "LOW" : "MODERATE");

        String[] difficulties = {"LOW", "MODERATE", "HIGH", "COMPLEX"};
        String difficulty = impacted ? difficulties[rng.nextInt(3) + 1] : difficulties[rng.nextInt(2)];

        String prediction = impacted ? "IMPACTED_CANINE" : "NORMAL_ERUPTION";
        String recommendation = impacted
                ? "Refer for orthodontic review and correlate with the complete CBCT study before intervention."
                : "Continue routine clinical monitoring and correlate with the complete CBCT study.";

        String[] sectors = {"Maxillary Right Quadrant (Sector 1)", "Maxillary Left Quadrant (Sector 2)"};
        String sector = sectors[rng.nextInt(2)];

        int sliceIndex = rng.nextInt(12);
        int x = sector.contains("Right") ? 100 + rng.nextInt(50) : 350 + rng.nextInt(50);
        int y = 150 + rng.nextInt(100);
        
        Map<String, Object> boundingBox = new HashMap<>();
        boundingBox.put("sliceIndex", sliceIndex);
        boundingBox.put("x", x);
        boundingBox.put("y", y);
        boundingBox.put("width", 60);
        boundingBox.put("height", 80);

        Map<String, Object> predictionMap = new HashMap<>();
        predictionMap.put("prediction", prediction);
        predictionMap.put("confidence", confidence);
        predictionMap.put("angle", angle);
        predictionMap.put("eruptionProbability", eruptionProbability);
        predictionMap.put("threatLevel", risk);
        predictionMap.put("difficulty", difficulty);
        predictionMap.put("sectorLocation", sector);
        predictionMap.put("clinicalRecommendation", recommendation);
        predictionMap.put("boundingBox", boundingBox);

        Map<String, Object> payload = new HashMap<>();
        payload.put("studyId", studyId.toString());
        payload.put("prediction", predictionMap);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("engine", "DemoPredictionEngine");
        metadata.put("device", "CPU");
        payload.put("metadata", metadata);

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate demo prediction JSON", e);
        }
    }
}
