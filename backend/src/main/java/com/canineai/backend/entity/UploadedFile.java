package com.canineai.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "uploaded_files")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadedFile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "CHAR(36)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false, columnDefinition = "CHAR(36)")
    private UploadSession session;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "storage_location_path", nullable = false)
    private String storageLocationPath;

    @Column(name = "file_size_bytes", nullable = false)
    private long fileSizeBytes;

    @Column(name = "sop_instance_uid")
    private String sopInstanceUid;

    @Column(name = "checksum_sha256")
    private String checksumSha256;
}
