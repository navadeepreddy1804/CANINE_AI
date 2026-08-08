package com.canineai.backend.service;

import com.canineai.backend.entity.*;
import com.canineai.backend.repository.ClinicalReportRepository;
import com.canineai.backend.repository.StudyRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PersistedAnalysisReportService {
    private final ClinicalReportRepository reports;
    private final StudyRepository studies;
    private final ObjectMapper objectMapper;

    @Transactional
    public void createIfAbsent(UUID studyId, String predictionJson, String author, PredictionSource source) throws Exception {
        Study study = studies.findById(studyId).orElseThrow();
        if (!reports.existsByStudyIdAndDeletedFalse(studyId)) {
            Map<String, Object> payload = objectMapper.readValue(predictionJson, new TypeReference<>() {});
            @SuppressWarnings("unchecked")
            Map<String, Object> prediction = (Map<String, Object>) payload.get("prediction");
            Patient patient = study.getPatient();

            StringBuilder markdown = new StringBuilder();
            markdown.append("# CanineAI Clinical Analysis Report\n\n");
            
            // Header
            markdown.append("## Patient\n");
            markdown.append("- Name: ").append(patient.getFullName()).append("\n");
            markdown.append("- Patient ID: ").append(patient.getHospitalPatientId()).append("\n");
            markdown.append("- Age / Gender: ").append(patient.getAge()).append(" / ").append(patient.getGender()).append("\n\n");
            
            markdown.append("## Study & Imaging\n");
            markdown.append("- Study ID: ").append(study.getId()).append("\n");
            markdown.append("- Modality: ").append(study.getModality()).append("\n");
            markdown.append("- Analysis Timestamp: ").append(LocalDateTime.now()).append("\n\n");
            markdown.append("---\n\n");

            // Extract generic prediction fields
            Object predClass = prediction != null ? prediction.getOrDefault("prediction", "—") : "—";
            Object confidence = prediction != null ? prediction.getOrDefault("confidence", "—") : "—";
            Object recommendation = prediction != null ? prediction.getOrDefault("clinicalRecommendation", "—") : "—";

            markdown.append("## Section 1: Clinical Diagnostic Assessment\n");
            markdown.append("- **Diagnostic Classification**: ").append(predClass).append("\n");
            markdown.append("- **Confidence**: ").append(confidence).append("\n");
            markdown.append("\n### Clinical Recommendation\n").append(recommendation).append("\n\n");

            // Check if findings exist
            if (prediction != null && prediction.containsKey("findings")) {
                Object findingsObj = prediction.get("findings");
                if (findingsObj instanceof List) {
                    List<?> findings = (List<?>) findingsObj;
                    markdown.append("### AI Findings & Reasoning\n");
                    for (Object finding : findings) {
                        markdown.append("- ").append(finding).append("\n");
                    }
                    markdown.append("\n");
                }
            }

            markdown.append("---\n\n");
            markdown.append("## Section 2: Detailed Localization & Measurements\n\n");

            if (prediction != null && prediction.containsKey("detectedCanines")) {
                Object caninesObj = prediction.get("detectedCanines");
                if (caninesObj instanceof List) {
                    List<?> canines = (List<?>) caninesObj;
                    for (Object canineObj : canines) {
                        if (canineObj instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> canine = (Map<String, Object>) canineObj;
                            
                            Object fdi = canine.getOrDefault("fdi", "Unknown");
                            Object side = canine.getOrDefault("side", "Unknown");
                            
                            markdown.append("### Canine: Tooth ").append(fdi).append(" (").append(side).append(")\n\n");
                            markdown.append("| Measurement | Value |\n");
                            markdown.append("| --- | --- |\n");
                            
                            // Anatomical features
                            Object eruption = canine.getOrDefault("eruptionPrediction", "—");
                            Object conf = canine.getOrDefault("confidence", "—");
                            Object cx = canine.getOrDefault("centroidX", "—");
                            Object cy = canine.getOrDefault("centroidY", "—");
                            Object cz = canine.getOrDefault("centroidZ", "—");
                            Object angulation = canine.getOrDefault("angulation", "—");
                            Object depth = canine.getOrDefault("depthMm", "—");
                            Object distOcclusal = canine.getOrDefault("distanceToOcclusalPlaneMm", "—");
                            Object distMidline = canine.getOrDefault("distanceToMidlineMm", "—");
                            Object overlap = canine.getOrDefault("overlapWithLateralIncisor", "—");
                            Object rootForm = canine.getOrDefault("rootFormationPercentage", "—");
                            
                            markdown.append("| Eruption Prediction | ").append(eruption).append(" |\n");
                            markdown.append("| Local Confidence | ").append(conf).append(" |\n");
                            markdown.append("| Centroid (X, Y, Z) | [").append(cx).append(", ").append(cy).append(", ").append(cz).append("] |\n");
                            markdown.append("| Angulation | ").append(angulation).append("° |\n");
                            markdown.append("| Depth | ").append(depth).append(" mm |\n");
                            markdown.append("| Distance to Occlusal Plane | ").append(distOcclusal).append(" mm |\n");
                            markdown.append("| Distance to Midline | ").append(distMidline).append(" mm |\n");
                            markdown.append("| Lateral Incisor Overlap | ").append(overlap).append(" |\n");
                            markdown.append("| Root Formation | ").append(rootForm).append("% |\n");
                            
                            markdown.append("\n");
                        }
                    }
                }
            } else {
                // Fallback for real mode backwards compatibility
                Object canineToothName = prediction != null ? prediction.getOrDefault("canineToothName", "Maxillary Right Canine") : "Maxillary Right Canine";
                Object canineFdi = prediction != null ? prediction.getOrDefault("canineFdi", "13") : "13";
                Object canineVol = prediction != null ? prediction.getOrDefault("canineVolumeMm3", prediction.getOrDefault("volumeMm3", "—")) : "—";
                Object canineAngle = prediction != null ? prediction.getOrDefault("angle", "—") : "—";
                Object canineCentroid = prediction != null ? prediction.getOrDefault("canineCentroid", "—") : "—";
                
                markdown.append("- **Canine Identified**: ").append(canineToothName).append(" (FDI ").append(canineFdi).append(")\n");
                markdown.append("- **Anatomical Canine Volume**: ").append(canineVol).append(" mm³\n");
                markdown.append("- **3D PCA Angulation**: ").append(canineAngle).append("°\n");
                markdown.append("- **3D Centroid (X, Y, Z)**: ").append(canineCentroid).append("\n\n");
            }
            
            markdown.append("---\n\n");
            markdown.append("Generated by CanineAI Clinical Healthcare Platform");

            ClinicalReport report = ClinicalReport.builder().studyId(studyId).status(ReportStatus.DRAFT)
                    .reportStyle(ReportStyle.CLINICAL).reportMarkdown(markdown.toString()).activeProvider("toothseg-analysis")
                    .templateVersion("2.1").promptVersion("2.1").promptTemplateKey("toothseg-analysis").build();
            report.setCreatedAt(LocalDateTime.now()); report.setCreatedBy(author);
            report.setPredictionSource(source);
            reports.save(report);
        }
        study.setStatus(StudyStatus.COMPLETED);
        studies.save(study);
    }
}
