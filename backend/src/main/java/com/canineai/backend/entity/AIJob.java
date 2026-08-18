package com.canineai.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_jobs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIJob extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "CHAR(36)")
    private UUID id;

    @Column(name = "study_id", nullable = false, columnDefinition = "CHAR(36)")
    private UUID studyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false)
    private AiTaskType taskType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobState state;

    @Column(name = "active_model_name", nullable = false)
    private String activeModelName;

    @Column(name = "model_version", nullable = false)
    private String modelVersion;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "progress_percentage", nullable = false)
    private int progressPercentage;

    @Lob
    @Column(name = "result_json", length = 5000)
    private String resultJson;

    @Column(name = "current_stage")
    private String currentStage;

    @Enumerated(EnumType.STRING)
    @Column(name = "prediction_source")
    private PredictionSource predictionSource;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "worker_id")
    private String workerId;

    @Column(name = "claimed_at")
    private LocalDateTime claimedAt;

    @Column(name = "lease_expiry")
    private LocalDateTime leaseExpiry;

    @Builder.Default
    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Builder.Default
    @Column(name = "max_retries", nullable = false)
    private int maxRetries = 3;

    @Column(name = "segmentation_file_path")
    private String segmentationFilePath;

    @Lob
    @Column(name = "visualization_files_json", length = 2000)
    private String visualizationFilesJson;

    @Version
    private Long version;
}
