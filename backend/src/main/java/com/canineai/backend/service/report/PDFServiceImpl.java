package com.canineai.backend.service.report;

import com.canineai.backend.common.BusinessException;
import com.canineai.backend.dto.PersistedReportDto;
import com.canineai.backend.dto.ReportResponse;
import com.canineai.backend.entity.AIJob;
import com.canineai.backend.entity.Patient;
import com.canineai.backend.entity.Study;
import com.canineai.backend.entity.UploadedFile;
import com.canineai.backend.repository.AIJobRepository;
import com.canineai.backend.repository.StudyRepository;
import com.canineai.backend.repository.UploadedFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PDFServiceImpl implements PDFService {

    private final ClinicalReportService clinicalReportService;
    private final StudyRepository studyRepository;
    private final com.canineai.backend.repository.StudyStorageRepository studyStorageRepository;
    private final AIJobRepository aiJobRepository;
    private final UploadedFileRepository uploadedFileRepository;
    private final PDFExporter pdfExporter;

    @Override
    @Transactional(readOnly = true)
    public byte[] renderPersistedPdf(UUID reportId, String currentUser) {
        PersistedReportDto report = loadPersistedReport(reportId, currentUser);
        return pdfExporter.exportPdf(renderContent(report), report.getPreviewImagePaths());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canRenderPersistedPdf(UUID reportId, String currentUser) {
        loadPersistedReport(reportId, currentUser);
        return true;
    }

    private PersistedReportDto loadPersistedReport(UUID reportId, String currentUser) {
        ReportResponse report = clinicalReportService.getReportForOwner(reportId, currentUser);
        Study study = studyRepository.findById(report.getStudyId())
                .orElseThrow(() -> new BusinessException.ResourceNotFoundException("Study not found"));
        Patient patient = study.getPatient();
        AIJob job = aiJobRepository.findFirstByStudyIdAndDeletedFalseOrderByEndTimeDesc(study.getId()).orElse(null);

        return PersistedReportDto.builder()
                .reportId(report.getId())
                .reportMarkdown(report.getReportMarkdown())
                .reportCreatedAt(null)
                .doctorEmail(currentUser)
                .patientName(patient.getFullName())
                .patientId(patient.getHospitalPatientId())
                .patientDateOfBirth(patient.getDateOfBirth())
                .patientGender(patient.getGender() == null ? null : patient.getGender().name())
                .studyId(study.getId())
                .studyDate(study.getStudyDate())
                .modality(study.getModality())
                .studyDescription(study.getStudyDescription())
                .rows(study.getRows())
                .columns(study.getColumns())
                .voxelSize(study.getVoxelSize())
                .pixelSpacing(study.getPixelSpacing())
                .sliceThickness(study.getSliceThickness())
                .analysisCompletedAt(job == null ? null : job.getEndTime())
                .aiResultJson(job == null ? null : job.getResultJson())
                .previewImagePaths(loadPreviewImagePaths(study))
                .build();
    }

    private List<Path> loadPreviewImagePaths(Study study) {
        List<Path> paths = new java.util.ArrayList<>();
        if (study.getUploadSessionId() != null) {
            uploadedFileRepository.findBySessionId(study.getUploadSessionId()).stream()
                    .map(UploadedFile::getStorageLocationPath)
                    .filter(this::isPreviewImagePath)
                    .map(Path::of)
                    .filter(Files::isRegularFile)
                    .forEach(paths::add);
        }
        studyStorageRepository.findByStudyId(study.getId()).ifPresent(storage -> {
            if (storage.getPreviewImagePaths() != null && !storage.getPreviewImagePaths().isBlank()) {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    java.util.Map<String, String> map = mapper.readValue(storage.getPreviewImagePaths(), new com.fasterxml.jackson.core.type.TypeReference<>() {});
                    for (String val : map.values()) {
                        if (val != null) {
                            Path p = Path.of(val);
                            if (Files.isRegularFile(p) && !paths.contains(p)) {
                                paths.add(p);
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }
            if (paths.isEmpty() && storage.getStoragePath() != null) {
                Path previewDir = Path.of(storage.getStoragePath()).resolve("previews");
                if (Files.isDirectory(previewDir)) {
                    for (String name : List.of("axial.png", "coronal.png", "sagittal.png")) {
                        Path p = previewDir.resolve(name);
                        if (Files.isRegularFile(p) && !paths.contains(p)) {
                            paths.add(p);
                        }
                    }
                }
            }
        });
        return paths;
    }

    private boolean isPreviewImagePath(String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.toLowerCase();
        return normalized.endsWith(".png") || normalized.endsWith(".jpg") || normalized.endsWith(".jpeg");
    }

    private String renderContent(PersistedReportDto report) {
        StringBuilder sb = new StringBuilder();
        sb.append("# CanineAI Clinical Orthodontic Report\n\n");
        
        sb.append("## Patient Information\n");
        sb.append("| Field | Value |\n");
        sb.append("|---|---|\n");
        sb.append("| Name | ").append(safe(report.getPatientName())).append(" |\n");
        sb.append("| Patient ID | ").append(safe(report.getPatientId())).append(" |\n");
        sb.append("| Date of Birth | ").append(safe(report.getPatientDateOfBirth())).append(" |\n");
        sb.append("| Gender | ").append(safe(report.getPatientGender())).append(" |\n\n");

        sb.append("## Study Information\n");
        sb.append("| Field | Value |\n");
        sb.append("|---|---|\n");
        sb.append("| Study ID | ").append(safe(report.getStudyId())).append(" |\n");
        sb.append("| Study Date | ").append(safe(report.getStudyDate())).append(" |\n");
        sb.append("| Modality | ").append(safe(report.getModality())).append(" |\n\n");

        sb.append("## AI Prediction Summary\n");
        
        String aiJson = report.getAiResultJson();
        if (aiJson != null && !aiJson.isBlank()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(aiJson);
                com.fasterxml.jackson.databind.JsonNode pred = root.has("prediction") ? root.get("prediction") : root;
                
                String fdi = pred.has("fdiNumber") ? pred.get("fdiNumber").asText() : "13";
                String toothName = pred.has("toothName") ? pred.get("toothName").asText() : "Maxillary Right Canine";
                String status = pred.has("eruptionStatus") ? pred.get("eruptionStatus").asText() : "IMPACTED";
                String conf = pred.has("confidence") ? pred.get("confidence").asText() : "74";
                
                sb.append("| Metric | Result |\n");
                sb.append("|---|---|\n");
                sb.append("| Detected Tooth | ").append(toothName).append(" (FDI ").append(fdi).append(") |\n");
                sb.append("| Eruption Status | **").append(status).append("** |\n");
                sb.append("| AI Confidence | ").append(conf).append("% |\n\n");
                
                sb.append("### Clinical Measurements\n");
                sb.append("| Parameter | Value |\n");
                sb.append("|---|---|\n");
                if (pred.has("angulation")) sb.append("| Canine Angulation | ").append(pred.get("angulation").asText()).append(" degrees |\n");
                if (pred.has("volume")) sb.append("| Canine Volume | ").append(pred.get("volume").asText()).append(" mm3 |\n");
                if (pred.has("distanceToMidline")) sb.append("| Distance to Midline | ").append(pred.get("distanceToMidline").asText()).append(" mm |\n");
                if (pred.has("distanceToOcclusalPlane")) sb.append("| Distance to Occlusal | ").append(pred.get("distanceToOcclusalPlane").asText()).append(" mm |\n");
                if (pred.has("archPosition")) sb.append("| Arch Position | ").append(pred.get("archPosition").asText()).append(" |\n");
                if (pred.has("crownPosition")) sb.append("| Crown Position | ").append(pred.get("crownPosition").asText()).append(" |\n\n");
                
                sb.append("### Clinical Findings\n");
                if (pred.has("clinicalFindings")) {
                    sb.append(pred.get("clinicalFindings").asText()).append("\n\n");
                }
                
                sb.append("### Recommendation\n");
                if (pred.has("clinicalRecommendation")) {
                    sb.append(pred.get("clinicalRecommendation").asText()).append("\n\n");
                }
                
            } catch (Exception e) {
                sb.append("- Failed to parse advanced AI findings.\n\n");
            }
        } else {
            sb.append("- Analysis pending or unavailable.\n\n");
        }
        
        sb.append("---\n");
        sb.append("**Report Generated:** ").append(safe(report.getAnalysisCompletedAt())).append("\n");
        sb.append("*AI MODE: DEMONSTRATION*\n");
        return sb.toString();
    }

    private String safe(Object value) {
        return value == null ? "Not recorded" : value.toString();
    }
}
