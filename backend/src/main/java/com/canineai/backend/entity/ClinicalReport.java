package com.canineai.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "clinical_reports")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClinicalReport extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "CHAR(36)")
    private UUID id;

    @Column(name = "study_id", nullable = false, columnDefinition = "CHAR(36)")
    private UUID studyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_style", nullable = false)
    private ReportStyle reportStyle;

    @Lob
    @Column(name = "report_markdown", length = 10000)
    private String reportMarkdown;

    @Column(name = "active_provider", nullable = false)
    private String activeProvider;

    @Column(name = "template_version", nullable = false)
    private String templateVersion;

    @Column(name = "prompt_version", nullable = false)
    private String promptVersion;

    @Column(name = "prompt_template_key", nullable = false)
    private String promptTemplateKey;

    @Column(name = "prompt_token_usage")
    private Integer promptTokenUsage;

    @Column(name = "completion_token_usage")
    private Integer completionTokenUsage;

    @Column(name = "generation_latency_ms")
    private Long generationLatencyMs;

    @Column(name = "doctor_comments", length = 1000)
    private String doctorComments;

    @Enumerated(EnumType.STRING)
    @Column(name = "prediction_source")
    private PredictionSource predictionSource;

    @Column(name = "approved_by")
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Version
    private Long version;
}
