package com.canineai.backend.controller;

import com.canineai.backend.common.BusinessException;
import com.canineai.backend.service.report.PDFService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@Tag(name = "Clinical PDF Exporter", description = "Endpoints to convert compiled markdown reports to signed PDF formats")
public class PDFReportController {

    private final PDFService pdfService;

    @GetMapping("/{reportId}/pdf")
    @Operation(summary = "Download persisted clinical PDF", description = "Renders and returns a PDF exclusively from owner-scoped persisted report data.")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORTHODONTIST', 'CLINICIAN') or hasAuthority('reports:read')")
    public ResponseEntity<byte[]> downloadPersistedPdf(@PathVariable UUID reportId, Principal principal) {
        byte[] pdfData = pdfService.renderPersistedPdf(reportId, authenticatedUsername(principal));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "CanineAI_ClinicalReport_" + reportId + ".pdf");
        return ResponseEntity.ok().headers(headers).body(pdfData);
    }

    private String authenticatedUsername(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new BusinessException.UnauthorizedException("Authenticated user is required");
        }
        return principal.getName();
    }
}
