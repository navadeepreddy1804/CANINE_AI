package com.canineai.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "analysis_history")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "CHAR(36)")
    private UUID id;

    @Column(name = "study_id", nullable = false, unique = true, columnDefinition = "CHAR(36)")
    private UUID studyId;

    @Column(name = "patient_id", nullable = false, columnDefinition = "CHAR(36)")
    private UUID patientId;

    @Column(name = "patient_name")
    private String patientName;

    @Column(name = "patient_display_id")
    private String patientDisplayId;

    @Column(name = "study_display_id")
    private String studyDisplayId;

    @Column(name = "prediction")
    private String prediction;

    @Column(name = "confidence")
    private String confidence;

    @Column(name = "status")
    private String status;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;
}
