package com.canineai.backend.service.report;

import com.canineai.backend.dto.ReportResponse;
import java.util.List;
import java.util.UUID;

public interface ClinicalReportService {

    /**
     * Lists reports visible to the authenticated doctor. Visibility is derived from
     * Report -> Study -> Patient.createdBy ownership.
     */
    List<ReportResponse> getReportsForOwner(String currentUser);

    /**
     * Retrieves a report only when it belongs to the authenticated doctor.
     */
    ReportResponse getReportForOwner(UUID reportId, String currentUser);

    /**
     * Retrieves a study report only when the study belongs to the authenticated doctor.
     */
    ReportResponse getReportByStudyIdForOwner(UUID studyId, String currentUser);
}
