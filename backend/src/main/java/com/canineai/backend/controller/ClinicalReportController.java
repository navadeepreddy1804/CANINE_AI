package com.canineai.backend.controller;

import com.canineai.backend.common.ApiResponse;
import com.canineai.backend.common.BusinessException;
import com.canineai.backend.dto.ReportResponse;
import com.canineai.backend.service.report.ClinicalReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@Tag(name = "Clinical Reports", description = "Owner-scoped persisted clinical report endpoints")
public class ClinicalReportController {

    private final ClinicalReportService reportService;

    @GetMapping("/{id}")
    @Operation(summary = "Fetch persisted report", description = "Retrieves a persisted report owned by the authenticated doctor.")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORTHODONTIST', 'CLINICIAN') or hasAuthority('reports:read')")
    public ResponseEntity<ApiResponse<ReportResponse>> getReport(
            @PathVariable("id") UUID id,
            Principal principal) {
        ReportResponse response = reportService.getReportForOwner(id, authenticatedUsername(principal));
        return ResponseEntity.ok(ApiResponse.success(response, "Report details retrieved successfully"));
    }

    @GetMapping
    @Operation(summary = "List owned reports", description = "Returns persisted reports for patients created by the authenticated doctor.")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORTHODONTIST', 'CLINICIAN') or hasAuthority('reports:read')")
    public ResponseEntity<ApiResponse<List<ReportResponse>>> getReports(Principal principal) {
        List<ReportResponse> response = reportService.getReportsForOwner(authenticatedUsername(principal));
        return ResponseEntity.ok(ApiResponse.success(response, "Reports retrieved successfully"));
    }

    @GetMapping("/study/{studyId}")
    @Operation(summary = "Fetch persisted report by study ID", description = "Retrieves a persisted study report owned by the authenticated doctor.")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORTHODONTIST', 'CLINICIAN') or hasAuthority('reports:read')")
    public ResponseEntity<ApiResponse<ReportResponse>> getReportByStudyId(
            @PathVariable("studyId") UUID studyId,
            Principal principal) {
        ReportResponse response = reportService.getReportByStudyIdForOwner(studyId, authenticatedUsername(principal));
        return ResponseEntity.ok(ApiResponse.success(response, "Report details retrieved successfully"));
    }

    private String authenticatedUsername(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new BusinessException.UnauthorizedException("Authenticated user is required");
        }
        return principal.getName();
    }
}
