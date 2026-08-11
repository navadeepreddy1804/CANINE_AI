package com.canineai.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "studies")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Study extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "CHAR(36)")
    private UUID id;

    @Column(name = "upload_session_id", columnDefinition = "CHAR(36)")
    private UUID uploadSessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false, columnDefinition = "CHAR(36)")
    private Patient patient;

    @Column(name = "study_instance_uid", nullable = false, unique = true)
    private String studyInstanceUid;

    @Column(name = "study_date")
    private LocalDate studyDate;

    @Column(name = "study_time")
    private String studyTime;

    @Column(nullable = false)
    private String modality; // e.g. CT, DX

    @Column(name = "study_description")
    private String studyDescription;

    @Column
    private String manufacturer;

    @Column(name = "device_model")
    private String deviceModel;

    @Column(name = "voxel_size")
    private String voxelSize;

    @Column(name = "pixel_spacing")
    private String pixelSpacing;

    @Column(name = "slice_thickness")
    private Double sliceThickness;

    @Column(name = "rows_count")
    private Integer rows;

    @Column(name = "columns_count")
    private Integer columns;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StudyStatus status;

    public String getStudyDisplayId() {
        if (id == null) return "ST-00000000-0000";
        String dateStr = (studyDate != null ? studyDate : LocalDate.now()).toString().replace("-", "");
        String hex = id.toString().replace("-", "").substring(0, 4).toUpperCase();
        return "ST-" + dateStr + "-" + hex;
    }
}
