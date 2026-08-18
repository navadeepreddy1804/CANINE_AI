package com.canineai.backend.service.report;

import com.canineai.backend.common.BusinessException;
import com.canineai.backend.dto.ReportResponse;
import com.canineai.backend.entity.ClinicalReport;
import com.canineai.backend.entity.Study;
import com.canineai.backend.entity.AIJob;
import com.canineai.backend.repository.ClinicalReportRepository;
import com.canineai.backend.repository.StudyRepository;
import com.canineai.backend.repository.AIJobRepository;
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
public class ClinicalReportServiceImpl implements ClinicalReportService {

    private final ClinicalReportRepository reportRepository;
    private final StudyRepository studyRepository;
    private final AIJobRepository jobRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ReportResponse> getReportsForOwner(String currentUser) {
        return reportRepository.findAllOwned(currentUser).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ReportResponse getReportForOwner(UUID reportId, String currentUser) {
        ClinicalReport report = reportRepository.findByIdOwned(reportId, currentUser)
                .orElseThrow(() -> new BusinessException.ResourceNotFoundException("Clinical Report not found"));
        return mapToResponse(report);
    }

    @Override
    @Transactional(readOnly = true)
    public ReportResponse getReportByStudyIdForOwner(UUID studyId, String currentUser) {
        ClinicalReport report = reportRepository.findByStudyIdOwned(studyId, currentUser)
                .orElseGet(() -> reportRepository.findFirstByStudyIdAndDeletedFalseOrderByCreatedAtDesc(studyId)
                        .orElseThrow(() -> new BusinessException.ResourceNotFoundException("Clinical Report not found for study: " + studyId)));
        return mapToResponse(report);
    }

    private ReportResponse mapToResponse(ClinicalReport report) {
        LocalDateTime timestamp = report.getApprovedAt() != null ? report.getApprovedAt() : report.getCreatedAt();
        String formattedTime = "Date unavailable";
        if (timestamp != null) {
            try {
                formattedTime = timestamp.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
            } catch (Exception ignored) {}
        }

        ReportResponse.ReportResponseBuilder builder = ReportResponse.builder()
                .id(report.getId())
                .studyId(report.getStudyId())
                .status(report.getStatus())
                .reportStyle(report.getReportStyle())
                .reportMarkdown(report.getReportMarkdown())
                .activeProvider(report.getActiveProvider())
                .promptTemplateKey(report.getPromptTemplateKey())
                .templateVersion(report.getTemplateVersion())
                .generationLatencyMs(report.getGenerationLatencyMs())
                .doctorComments(report.getDoctorComments())
                .approvedBy(report.getApprovedBy())
                .approvedAt(timestamp)
                .formattedApprovedAt(formattedTime)
                .minConfidenceThreshold(63);

        try {
            studyRepository.findById(report.getStudyId()).ifPresent(study -> {
                builder.studyDate(study.getStudyDate() != null ? study.getStudyDate().toString() : null);
                builder.studyDisplayId(study.getStudyDisplayId());
                if (study.getPatient() != null) {
                    builder.patientId(study.getPatient().getId());
                    builder.patientName(study.getPatient().getFullName());
                    builder.patientDisplayId(study.getPatient().getHospitalPatientId());
                }
            });

            jobRepository.findFirstByStudyIdAndDeletedFalseOrderByEndTimeDesc(report.getStudyId()).ifPresent(job -> {
                if (job.getResultJson() != null && !job.getResultJson().isBlank()) {
                    try {
                        builder.aiResultJson(job.getResultJson());
                        Map<String, Object> payload = objectMapper.readValue(job.getResultJson(), new TypeReference<>() {});
                        Map<String, Object> predMap = (Map<String, Object>) payload.get("prediction");
                        if (predMap == null) {
                            predMap = payload;
                        }
                        if (predMap != null) {
                            if (predMap.containsKey("prediction")) builder.prediction(String.valueOf(predMap.get("prediction")));
                            if (predMap.containsKey("confidence")) builder.confidence(String.valueOf(predMap.get("confidence")));
                            if (predMap.containsKey("difficulty")) builder.difficulty(String.valueOf(predMap.get("difficulty")));
                            if (predMap.containsKey("rootResorptionRisk")) builder.rootResorptionRisk(String.valueOf(predMap.get("rootResorptionRisk")));
                            if (predMap.containsKey("clinicalRecommendation")) builder.clinicalRecommendation(String.valueOf(predMap.get("clinicalRecommendation")));

                            if (predMap.containsKey("canineToothName")) builder.canineToothName(String.valueOf(predMap.get("canineToothName")));
                            if (predMap.containsKey("canineFdi")) builder.canineFdi(String.valueOf(predMap.get("canineFdi")));
                            if (predMap.containsKey("sectorLocation")) builder.canineSector(String.valueOf(predMap.get("sectorLocation")));
                            if (predMap.containsKey("canineCentroid")) builder.canineCentroid(String.valueOf(predMap.get("canineCentroid")));

                            if (predMap.containsKey("canineVolumeMm3") && predMap.get("canineVolumeMm3") instanceof Number num) {
                                builder.canineVolumeMm3(num.doubleValue());
                            }
                            if (predMap.containsKey("angle") && predMap.get("angle") instanceof Number num) {
                                builder.canineAngulation(num.doubleValue());
                            }
                            if (predMap.containsKey("toothCount") && predMap.get("toothCount") instanceof Number num) {
                                builder.totalTeethCount(num.intValue());
                            }
                            if (predMap.containsKey("maxillaryTeethCount") && predMap.get("maxillaryTeethCount") instanceof Number num) {
                                builder.maxillaryTeethCount(num.intValue());
                            }
                            if (predMap.containsKey("mandibularTeethCount") && predMap.get("mandibularTeethCount") instanceof Number num) {
                                builder.mandibularTeethCount(num.intValue());
                            }

                            if (predMap.get("boundingBox") instanceof Map<?, ?> bb) {
                                if (bb.get("sliceIndex") instanceof Number sIdx) builder.boundingBoxSliceIndex(sIdx.intValue());
                                if (bb.get("x") instanceof Number x) builder.boundingBoxX(x.doubleValue());
                                if (bb.get("y") instanceof Number y) builder.boundingBoxY(y.doubleValue());
                                if (bb.get("width") instanceof Number w) builder.boundingBoxWidth(w.doubleValue());
                                if (bb.get("height") instanceof Number h) builder.boundingBoxHeight(h.doubleValue());
                            }
                        }
                    } catch (Exception ex) {
                        // ignore parsing error
                    }
                }
            });
        } catch (Exception e) {
            // safe fallback
        }

        return builder.build();
    }
}
