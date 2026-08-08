package com.canineai.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "upload_sessions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "CHAR(36)")
    private UUID id;

    @Column(name = "patient_id", nullable = false, columnDefinition = "CHAR(36)")
    private UUID patientId;

    @Column(nullable = false)
    private String username; // Operator email

    @Column(name = "total_size_bytes", nullable = false)
    private long totalSizeBytes;

    @Column(name = "total_files_count")
    private int totalFilesCount;

    @Column(name = "uploaded_files_count")
    private int uploadedFilesCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StudyStatus status;

    @Column(name = "checksum_md5")
    private String checksumMd5;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    /**
     * Calculates dynamic progress percentage.
     */
    public int getProgressPercentage() {
        if (totalSizeBytes <= 0) return 0;
        if (status == StudyStatus.COMPLETED) return 100;
        if (totalFilesCount <= 0) return 0;
        return (int) (((double) uploadedFilesCount / totalFilesCount) * 100);
    }
}
