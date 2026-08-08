package com.canineai.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "series")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Series extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "CHAR(36)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_id", nullable = false, columnDefinition = "CHAR(36)")
    private Study study;

    @Column(name = "series_instance_uid", nullable = false, unique = true)
    private String seriesInstanceUid;

    @Column(name = "series_number")
    private Integer seriesNumber;

    @Column(name = "series_description")
    private String seriesDescription;

    @Column(name = "slice_count", nullable = false)
    private int sliceCount;
}
