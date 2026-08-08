package com.canineai.webapp.controller;

import com.canineai.webapp.client.BackendClient;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Controller
public class UploadController {

    private final BackendClient backendClient;

    public UploadController(BackendClient backendClient) {
        this.backendClient = backendClient;
    }

    // ... existing showUploadWorkspace and saveUploadDetails ...
    @GetMapping("/upload")
    public String showUploadWorkspace(
            @RequestParam(value = "patientId", required = false) String patientId,
            HttpSession session,
            Model model) {

        if (session.getAttribute("authenticated") == null) {
            return "redirect:/login";
        }

        String accessToken = (String) session.getAttribute("accessToken");
        List<Map<String, Object>> patientDb = backendClient.getPatients(accessToken);

        if (patientDb.isEmpty()) {
            model.addAttribute("noPatients", true);
            return "upload";
        }

        model.addAttribute("noPatients", false);

        // Find patient context dynamically
        Map<String, Object> selectedPatient = null;
        if (patientId != null) {
            selectedPatient = patientDb.stream()
                    .filter(p -> p.get("id").toString().equals(patientId))
                    .findFirst()
                    .orElse(null);
        }

        if (selectedPatient != null) {
            model.addAttribute("patient", selectedPatient);
            model.addAttribute("studyUid", null);
            model.addAttribute("seriesUid", null);
            model.addAttribute("sliceCount", null);
        } else {
            model.addAttribute("patient", null);
        }
        model.addAttribute("patients", patientDb);
        
        // Upload-session history is not available as a backend collection endpoint.
        model.addAttribute("uploads", List.of());
        
        return "upload";
    }

    @PostMapping("/upload/file")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam("patientId") String patientId,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            HttpSession session) {

        Map<String, Object> result = new HashMap<>();
        if (session.getAttribute("authenticated") == null) {
            result.put("success", false);
            result.put("message", "Unauthorized session");
            return ResponseEntity.status(401).body(result);
        }

        String accessToken = (String) session.getAttribute("accessToken");
        String filename = file.getOriginalFilename();
        if (filename == null) {
            filename = "CBCT_Study_Scan.zip";
        }

        try {
            byte[] fileBytes = file.getBytes();
            String lowerName = filename.toLowerCase();

            Map<String, Object> sessionResponse;

            if (lowerName.endsWith(".zip")) {
                sessionResponse = backendClient.uploadZipStream(patientId, fileBytes, accessToken);
            } else if (lowerName.endsWith(".nii") || lowerName.endsWith(".nii.gz") || lowerName.endsWith(".dcm")
                    || lowerName.endsWith(".mha") || lowerName.endsWith(".mhd") || lowerName.endsWith(".nrrd")) {
                Map<String, Object> initResp = backendClient.initializeUploadSession(patientId, fileBytes.length, 1, accessToken);
                if (initResp == null || !initResp.containsKey("id")) {
                    throw new RuntimeException("Failed to allocate upload session on backend API");
                }
                String sessionId = (String) initResp.get("id");
                backendClient.uploadFileChunk(sessionId, filename, fileBytes, accessToken);
                sessionResponse = initResp;
            } else {
                result.put("success", false);
                result.put("message", "Unsupported study. Upload DICOM, ZIP, NIfTI (.nii/.nii.gz), MetaImage (.mha/.mhd), or NRRD.");
                return ResponseEntity.badRequest().body(result);
            }

            result.put("success", true);
            result.put("session", sessionResponse);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Upload failed: " + e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

    @GetMapping("/upload/status/{sessionId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getUploadStatus(
            @PathVariable("sessionId") String sessionId,
            HttpSession session) {

        if (session.getAttribute("authenticated") == null) {
            return ResponseEntity.status(401).build();
        }

        String accessToken = (String) session.getAttribute("accessToken");
        Map<String, Object> status = backendClient.getUploadSessionStatus(sessionId, accessToken);
        return ResponseEntity.ok(status);
    }

    @GetMapping(value = "/studies/{id}/previews/{type}", produces = org.springframework.http.MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    public ResponseEntity<byte[]> getPreviewImage(
            @PathVariable("id") String id,
            @PathVariable("type") String type,
            HttpSession session) {
        log.info("Proxy preview request received: studyId={}, type={}", id, type);
        String accessToken = (String) session.getAttribute("accessToken");
        if (accessToken == null) {
            log.warn("Preview request denied due to missing access token: studyId={}, type={}", id, type);
            return ResponseEntity.status(401).build();
        }

        byte[] imageBytes = backendClient.getPreviewImage(id, type, accessToken);
        if (imageBytes == null || imageBytes.length == 0) return ResponseEntity.notFound().build();

        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.IMAGE_PNG)
                .body(imageBytes);
    }

    @GetMapping(value = "/studies/{id}/previews/{type}/{index}", produces = org.springframework.http.MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    public ResponseEntity<byte[]> getIndexedPreviewImage(
            @PathVariable("id") String id,
            @PathVariable("type") String type,
            @PathVariable("index") int index,
            HttpSession session) {
        log.info("Proxy indexed preview request received: studyId={}, type={}, index={}", id, type, index);
        String accessToken = (String) session.getAttribute("accessToken");
        if (accessToken == null) {
            log.warn("Indexed preview request denied due to missing access token: studyId={}, type={}, index={}", id, type, index);
            return ResponseEntity.status(401).build();
        }

        byte[] imageBytes = backendClient.getIndexedPreviewImage(id, type, index, accessToken);
        if (imageBytes == null || imageBytes.length == 0) return ResponseEntity.notFound().build();

        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.IMAGE_PNG)
                .body(imageBytes);
    }

    @GetMapping(value = "/studies/{id}/dicom/list", produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<String>> getDicomList(
            @PathVariable("id") String id,
            HttpSession session) {
        log.info("Proxy DICOM list request received: studyId={}", id);
        String accessToken = (String) session.getAttribute("accessToken");
        if (accessToken == null) return ResponseEntity.status(401).build();

        List<String> files = backendClient.getDicomList(id, accessToken);
        return ResponseEntity.ok(files);
    }

    @GetMapping(value = "/studies/{id}/dicom/{filename}", produces = org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ResponseBody
    public ResponseEntity<byte[]> getDicomFile(
            @PathVariable("id") String id,
            @PathVariable("filename") String filename,
            HttpSession session) {
        String accessToken = (String) session.getAttribute("accessToken");
        if (accessToken == null) return ResponseEntity.status(401).build();

        byte[] fileBytes = backendClient.getDicomFile(id, filename, accessToken);
        if (fileBytes == null || fileBytes.length == 0) return ResponseEntity.notFound().build();

        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
                .body(fileBytes);
    }

    @GetMapping("/upload/studies/{patientId}")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getPatientStudies(
            @PathVariable("patientId") String patientId,
            HttpSession session) {

        if (session.getAttribute("authenticated") == null) {
            return ResponseEntity.status(401).build();
        }

        String accessToken = (String) session.getAttribute("accessToken");
        List<Map<String, Object>> studies = backendClient.getPatientStudies(patientId, accessToken);
        return ResponseEntity.ok(studies);
    }

    @PostMapping("/upload/initialize")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> initializeSession(
            @RequestParam("patientId") String patientId,
            @RequestParam("totalSize") long totalSize,
            @RequestParam("totalFiles") int totalFiles,
            HttpSession session) {

        if (session.getAttribute("authenticated") == null) {
            return ResponseEntity.status(401).build();
        }

        String accessToken = (String) session.getAttribute("accessToken");
        Map<String, Object> resp = backendClient.initializeUploadSession(patientId, totalSize, totalFiles, accessToken);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/upload/chunk")
    @ResponseBody
    public ResponseEntity<Void> uploadChunk(
            @RequestParam("sessionId") String sessionId,
            @RequestParam("fileName") String fileName,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            HttpSession session) {

        if (session.getAttribute("authenticated") == null) {
            return ResponseEntity.status(401).build();
        }

        String accessToken = (String) session.getAttribute("accessToken");
        try {
            backendClient.uploadFileChunk(sessionId, fileName, file.getBytes(), accessToken);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to upload chunk proxy: {}", e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }
}
