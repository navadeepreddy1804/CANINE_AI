package com.canineai.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "study_storage_records")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudyStorage extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "CHAR(36)")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_id", nullable = false, columnDefinition = "CHAR(36)")
    private Study study;

    @Column(name = "upload_session_id", nullable = false, columnDefinition = "CHAR(36)")
    private UUID uploadSessionId;

    @Column(name = "storage_path", nullable = false, length = 1000)
    private String storagePath;

    @Column(name = "file_count", nullable = false)
    private int fileCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "upload_status", nullable = false)
    private StudyStatus uploadStatus;

    @Column(name = "preview_image_paths", length = 2000)
    private String previewImagePaths;

    @Column(name = "report_path", length = 1000)
    private String reportPath;
}
