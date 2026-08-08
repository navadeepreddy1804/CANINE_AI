package com.canineai.webapp.controller;

import com.canineai.webapp.client.BackendClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

@lombok.extern.slf4j.Slf4j
@Controller
public class AnalysisController {

    private final BackendClient backendClient;

    public AnalysisController(BackendClient backendClient) {
        this.backendClient = backendClient;
    }

    @GetMapping("/analysis")
    public String showAnalysisWorkspace(
            @RequestParam(value = "patientId", required = false) String patientId,
            @RequestParam(value = "studyId", required = false) String studyId,
            HttpSession session,
            Model model) {

        if (session.getAttribute("authenticated") == null) {
            return "redirect:/login";
        }

        String accessToken = (String) session.getAttribute("accessToken");
        List<Map<String, Object>> patientDb = backendClient.getPatients(accessToken);

        if (patientDb.isEmpty()) {
            model.addAttribute("noPatients", true);
            model.addAttribute("noScans", true);
            return "analysis";
        }

        model.addAttribute("noPatients", false);
        model.addAttribute("patients", patientDb);

        // Find patient context dynamically
        Map<String, Object> selectedPatient = null;
        if (patientId != null) {
            selectedPatient = patientDb.stream()
                    .filter(p -> p.get("id").toString().equals(patientId))
                    .findFirst()
                    .orElse(null);
        }

        // If patient not identified yet and studyId is provided, find owning patient
        if (selectedPatient == null && studyId != null) {
            for (Map<String, Object> p : patientDb) {
                String pId = p.get("id").toString();
                try {
                    List<Map<String, Object>> pStudies = backendClient.getPatientStudies(pId, accessToken);
                    if (pStudies.stream().anyMatch(s -> studyId.equals(String.valueOf(s.get("id"))))) {
                        selectedPatient = p;
                        break;
                    }
                } catch (Exception ignored) {
                }
            }
        }

        if (selectedPatient == null) {
            selectedPatient = patientDb.get(0); // Default to first
        }

        model.addAttribute("patient", selectedPatient);

        // Check if this patient has any scans uploaded
        String selectedPatientId = selectedPatient.get("id").toString();
        List<Map<String, Object>> dbStudies = backendClient.getPatientStudies(selectedPatientId, accessToken);

        if (dbStudies.isEmpty()) {
            model.addAttribute("noScans", true);
            return "analysis";
        }

        model.addAttribute("noScans", false);

        Map<String, Object> scan = dbStudies.stream()
                .filter(study -> studyId != null && studyId.equals(String.valueOf(study.get("id"))))
                .findFirst()
                .orElseGet(() -> dbStudies.isEmpty() ? null : dbStudies.get(0));

        model.addAttribute("studyId", scan != null ? scan.get("id") : null);
        model.addAttribute("study", scan);
        model.addAttribute("studySliceCount", scan != null ? scan.get("sliceCount") : 0);
        
        Map<String, Object> persistedReport = null;
        if (scan != null && scan.get("id") != null && ("COMPLETED".equals(String.valueOf(scan.get("status"))) || "REPORT_GENERATED".equals(String.valueOf(scan.get("status"))))) {
            try {
                persistedReport = backendClient.getReportByStudyId(scan.get("id").toString(), accessToken);
            } catch (Exception ignored) {
                // No report found yet for this study
            }
        }

        model.addAttribute("persistedReport", persistedReport);
        model.addAttribute("aiEngineName", persistedReport != null ? "ToothSeg v2.1 (Multi-Stage nnUNet)" : null);
        model.addAttribute("modelVersion", persistedReport != null ? "v2.1.0" : null);
        model.addAttribute("prediction", persistedReport != null ? persistedReport.get("prediction") : null);

        return "analysis";
    }

    @PostMapping("/analysis/complete")
    @ResponseBody
    public ResponseEntity<Void> completeAnalysis(
            @RequestParam("patientId") String patientId,
            HttpSession session) {

        String accessToken = (String) session.getAttribute("accessToken");
        List<Map<String, Object>> dbStudies = backendClient.getPatientStudies(patientId, accessToken);
        if (!dbStudies.isEmpty()) {
            String studyId = dbStudies.get(0).get("id").toString();
            backendClient.triggerWorkflow(patientId, studyId, accessToken);
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/analysis/jobs")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> submitAnalysisJob(@RequestParam("studyId") String studyId, HttpSession session) {
        log.info("[Spring WebApp] POST /analysis/jobs received for studyId={}", studyId);
        String accessToken = (String) session.getAttribute("accessToken");
        Map<String, Object> job = backendClient.submitAiJob(studyId, accessToken);
        
        if (job == null) {
            log.error("[Spring WebApp] Failed to submit AI job. Backend returned null.");
            return ResponseEntity.status(500).body(Map.of("error", "Unable to trigger AI workflow."));
        }
        
        log.info("[Spring WebApp] AI job submitted successfully: jobId={}", job.get("id"));
        return ResponseEntity.ok(job);
    }

    @GetMapping("/analysis/jobs/{jobId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAnalysisJob(@org.springframework.web.bind.annotation.PathVariable("jobId") String jobId, HttpSession session) {
        String accessToken = (String) session.getAttribute("accessToken");
        Map<String, Object> progress = backendClient.getAiJobProgress(jobId, accessToken);
        
        if (progress == null) {
            log.warn("[Spring WebApp] Failed to fetch progress for AI job: {}", jobId);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to retrieve job progress from backend."));
        }
        
        if ("COMPLETED".equals(String.valueOf(progress.get("state")))) {
            try {
                Map<String, Object> jobDetails = backendClient.getAiJob(jobId, accessToken);
                if (jobDetails != null) {
                    Object resJson = jobDetails.get("resultJson") != null ? jobDetails.get("resultJson") : jobDetails.get("result");
                    if (resJson != null) {
                        progress.put("result", resJson);
                        progress.put("resultJson", resJson);
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to retrieve complete job details on completion for job {}: {}", jobId, e.getMessage());
            }
        }
        return ResponseEntity.ok(progress);
    }

    @PostMapping("/analysis/jobs/{jobId}/cancel")
    @ResponseBody
    public ResponseEntity<Void> cancelAnalysisJob(@org.springframework.web.bind.annotation.PathVariable("jobId") String jobId, HttpSession session) {
        String accessToken = (String) session.getAttribute("accessToken");
        backendClient.cancelAiJob(jobId, accessToken);
        return ResponseEntity.ok().build();
    }
}
