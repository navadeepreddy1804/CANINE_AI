package com.canineai.webapp.controller;

import com.canineai.webapp.client.BackendClient;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ReportController {

    private final BackendClient backendClient;

    @GetMapping("/reports")
    public String listReports(HttpSession session, Model model) {
        if (!isAuthenticated(session)) {
            return "redirect:/login";
        }

        try {
            List<Map<String, Object>> reports = backendClient.getReports(accessToken(session));
            model.addAttribute("reports", reports);
        } catch (RuntimeException exception) {
            model.addAttribute("reports", List.of());
            model.addAttribute("errorMessage", "Persisted reports could not be loaded.");
        }
        return "reports";
    }

    @GetMapping("/reports/{reportId}")
    public String reportDetails(@PathVariable("reportId") String reportId, HttpSession session, Model model) {
        if (!isAuthenticated(session)) {
            return "redirect:/login";
        }

        Map<String, Object> report = backendClient.getReport(reportId, accessToken(session));
        if (report == null) {
            return "redirect:/reports";
        }

        model.addAttribute("report", report);
        return "report-details";
    }

    @GetMapping("/reports/{reportId}/pdf")
    public ResponseEntity<byte[]> downloadReportPdf(@PathVariable("reportId") String reportId, HttpSession session) {
        if (!isAuthenticated(session)) {
            return ResponseEntity.status(401).build();
        }

        byte[] pdf = backendClient.downloadReportPdf(reportId, accessToken(session));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=CanineAI_ClinicalReport_" + reportId + ".pdf")
                .body(pdf);
    }

    @GetMapping(value = "/reports/study/{studyId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getReportByStudyId(@PathVariable("studyId") String studyId, HttpSession session) {
        if (!isAuthenticated(session)) {
            return ResponseEntity.status(401).build();
        }
        
        try {
            Map<String, Object> report = backendClient.getReportByStudyId(studyId, accessToken(session));
            if (report != null) {
                return ResponseEntity.ok(Map.of("data", report));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    private boolean isAuthenticated(HttpSession session) {
        return session.getAttribute("authenticated") != null;
    }

    private String accessToken(HttpSession session) {
        return (String) session.getAttribute("accessToken");
    }
}
