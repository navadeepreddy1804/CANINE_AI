package com.canineai.backend.repository;

import com.canineai.backend.entity.Series;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SeriesRepository extends JpaRepository<Series, UUID> {
    Optional<Series> findBySeriesInstanceUidAndDeletedFalse(String seriesInstanceUid);
    boolean existsBySeriesInstanceUidAndDeletedFalse(String seriesInstanceUid);
    List<Series> findByStudyId(UUID studyId);
}
