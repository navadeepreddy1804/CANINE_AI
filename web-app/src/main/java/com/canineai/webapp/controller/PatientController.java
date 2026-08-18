package com.canineai.webapp.controller;

import com.canineai.webapp.client.BackendClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/patients")
public class PatientController {

    private final BackendClient backendClient;

    public PatientController(BackendClient backendClient) {
        this.backendClient = backendClient;
    }

    @GetMapping
    public String getPatients(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "gender", required = false) String gender,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            HttpSession session, Model model) {
        if (session.getAttribute("authenticated") == null) {
            return "redirect:/login";
        }
        String accessToken = (String) session.getAttribute("accessToken");
        
        List<Map<String, Object>> patients = backendClient.getPatients(search, gender, status, page, size, accessToken);
        model.addAttribute("patients", patients);
        model.addAttribute("search", search);
        model.addAttribute("gender", gender);
        model.addAttribute("status", status);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        return "patients";
    }

    @GetMapping("/{id}")
    public String getPatientDetails(@PathVariable("id") String id, HttpSession session, Model model) {
        if (session.getAttribute("authenticated") == null) {
            return "redirect:/login";
        }

        String accessToken = (String) session.getAttribute("accessToken");
        Map<String, Object> patient = backendClient.getPatient(id, accessToken);

        if (patient == null) {
            return "redirect:/patients";
        }
        patient.put("dob", patient.get("dateOfBirth"));

        // Both endpoints are owner-scoped by the backend.  Filter the persisted report
        // list by this patient's persisted study identifiers; no report is generated or
        // reconstructed while viewing the patient record.
        List<Map<String, Object>> studies = backendClient.getPatientStudies(id, accessToken);
        Set<String> studyIds = studies.stream()
                .map(study -> study.get("id"))
                .filter(Objects::nonNull)
                .map(o -> o.toString().toLowerCase(java.util.Locale.ROOT))
                .collect(Collectors.toSet());
        List<Map<String, Object>> patientReports = backendClient.getReports(accessToken).stream()
                .filter(report -> report.get("studyId") != null)
                .filter(report -> studyIds.contains(report.get("studyId").toString().toLowerCase(java.util.Locale.ROOT)))
                .collect(Collectors.toList());

        model.addAttribute("patient", patient);
        model.addAttribute("studies", studies);
        model.addAttribute("reports", patientReports);

        return "patient-details";
    }

    @GetMapping("/new")
    public String showAddPatientForm(HttpSession session, Model model) {
        if (session.getAttribute("authenticated") == null) {
            return "redirect:/login";
        }

        String doctorName = (String) session.getAttribute("doctorName");

        Map<String, Object> patient = new HashMap<>();
        patient.put("id", "");
        patient.put("fullName", "");
        patient.put("age", "");
        patient.put("gender", "Male");
        patient.put("bloodGroup", "");
        patient.put("dob", "");
        patient.put("phone", "");
        patient.put("email", "");
        patient.put("orthodontist", doctorName != null ? doctorName : "");
        patient.put("medicalNotes", "");

        model.addAttribute("patient", patient);
        model.addAttribute("isEdit", false);
        return "patient-form";
    }

    @GetMapping("/{id}/edit")
    public String showEditPatientForm(@PathVariable("id") String id, HttpSession session, Model model) {
        if (session.getAttribute("authenticated") == null) {
            return "redirect:/login";
        }

        String accessToken = (String) session.getAttribute("accessToken");
        Map<String, Object> patient = backendClient.getPatient(id, accessToken);

        if (patient == null) {
            return "redirect:/patients";
        }
        patient.put("dob", patient.get("dateOfBirth"));

        model.addAttribute("patient", patient);
        model.addAttribute("isEdit", true);
        return "patient-form";
    }

    @PostMapping("/save")
    public String savePatient(
            @RequestParam(value = "id", required = false) String id,
            @RequestParam("fullName") String fullName,
            @RequestParam(value = "age", required = false) String age,
            @RequestParam("gender") String gender,
            @RequestParam("dob") String dob,
            @RequestParam("phone") String phone,
            @RequestParam("email") String email,
            @RequestParam("orthodontist") String orthodontist,
            @RequestParam("medicalNotes") String medicalNotes,
            @RequestParam("bloodGroup") String bloodGroup,
            HttpSession session,
            Model model) {

        if (session.getAttribute("authenticated") == null) {
            return "redirect:/login";
        }

        String accessToken = (String) session.getAttribute("accessToken");

        Map<String, Object> patientReq = new HashMap<>();
        patientReq.put("fullName", fullName);
        
        // Handle LocalDate parsing safely
        patientReq.put("dateOfBirth", dob); // "yyyy-MM-dd"
        patientReq.put("gender", "Male".equalsIgnoreCase(gender) ? "MALE" : "FEMALE");
        patientReq.put("phone", phone);
        patientReq.put("email", email);
        patientReq.put("address", "128 Clinical Parkway, Suite 4");
        patientReq.put("bloodGroup", bloodGroup);
        patientReq.put("medicalNotes", medicalNotes);
        patientReq.put("orthodontist", orthodontist);
        
        String hospitalName = (String) session.getAttribute("organizationName");
        patientReq.put("hospital", hospitalName != null ? hospitalName : "Metro Dental Diagnostics");
        patientReq.put("status", "ACTIVE");

        try {
            if (id == null || id.isBlank()) {
                // New Admission
                backendClient.createPatient(patientReq, accessToken);
            } else {
                // Update Existing
                Map<String, Object> existing = backendClient.getPatient(id, accessToken);
                if (existing != null) {
                    patientReq.put("hospitalPatientId", existing.get("hospitalPatientId"));
                    backendClient.updatePatient(id, patientReq, accessToken);
                }
            }
        } catch (Exception e) {
            String errorMsg = normalizeErrorMessage(e.getMessage(), "An unexpected error occurred. Please try again.");

            Map<String, Object> patient = new HashMap<>();
            patient.put("id", id != null ? id : "");
            patient.put("fullName", fullName);
            patient.put("age", age);
            patient.put("gender", gender);
            patient.put("bloodGroup", bloodGroup);
            patient.put("dob", dob);
            patient.put("phone", phone);
            patient.put("email", email);
            patient.put("orthodontist", orthodontist);
            patient.put("medicalNotes", medicalNotes);

            model.addAttribute("patient", patient);
            model.addAttribute("isEdit", id != null && !id.isBlank());
            model.addAttribute("errorMessage", errorMsg);
            return "patient-form";
        }

        return "redirect:/patients";
    }

    @PostMapping("/{id}/archive")
    public String toggleArchivePatient(@PathVariable("id") String id, HttpSession session) {
        if (session.getAttribute("authenticated") == null) {
            return "redirect:/login";
        }

        String accessToken = (String) session.getAttribute("accessToken");
        Map<String, Object> patient = backendClient.getPatient(id, accessToken);
        if (patient != null) {
            String currentStatus = pStr(patient.get("status"));
            String newStatus = "ACTIVE".equalsIgnoreCase(currentStatus) ? "ARCHIVED" : "ACTIVE";
            patient.put("status", newStatus);
            patient.put("dateOfBirth", patient.get("dateOfBirth")); // Preserve DOB format
            backendClient.updatePatient(id, patient, accessToken);
        }
        return "redirect:/patients";
    }

    private String pStr(Object obj) {
        return obj == null ? "" : obj.toString();
    }

    private String normalizeErrorMessage(String rawMessage, String defaultMessage) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return defaultMessage;
        }

        String value = rawMessage.replaceAll("\\s+", " ").trim();
        String lower = value.toLowerCase();
        if (lower.contains("required fields") || lower.contains("constraints validation failed") || lower.contains("is required") || lower.contains("notblank") || lower.contains("must not be blank") || lower.contains("must not be null")) {
            return "Required fields are missing.";
        }
        if (lower.contains("date of birth") || lower.contains("dateofbirth") || lower.contains("localdate") || lower.contains("invalid date") || lower.contains("date format") || lower.contains("could not read document") || lower.contains("datetimeparse") || lower.contains("parse")) {
            return "Invalid date of birth.";
        }
        if (lower.contains("age") && (lower.contains("between") || lower.contains("limit") || lower.contains("allowed"))) {
            return "Age must be between allowed limits.";
        }
        if (lower.contains("patient id already exists") || lower.contains("hospital patient id")) {
            return "Patient ID already exists.";
        }
        if (lower.contains("phone number already exists") || (lower.contains("phone") && lower.contains("already exists"))) {
            return "Phone number already exists.";
        }
        if (lower.contains("email already exists") || (lower.contains("email") && lower.contains("already exists"))) {
            return "Email already exists.";
        }
        if (lower.contains("unexpected error") || lower.contains("internal server error") || lower.contains("server error") || lower.contains("java.lang") || lower.contains("org.springframework") || lower.contains("sql") || lower.contains("hibernate") || lower.contains("stack trace") || lower.contains("caused by") || lower.contains("exception")) {
            return "An unexpected error occurred. Please try again.";
        }
        if (lower.contains("invalid phone") || lower.contains("phone number format") || lower.contains("phone format") || (lower.contains("phone") && lower.contains("format"))) {
            return "Invalid phone number format.";
        }
        if (lower.contains("invalid email") || lower.contains("email format") || (lower.contains("email") && lower.contains("invalid"))) {
            return "Invalid email address.";
        }
        if (lower.contains("not found")) {
            return "Patient record not found.";
        }

        return defaultMessage;
    }

    @PostMapping("/{id}/delete")
    public String deletePatient(
            @PathVariable("id") String id,
            HttpSession session,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        if (session.getAttribute("authenticated") == null) {
            return "redirect:/login";
        }

        String accessToken = (String) session.getAttribute("accessToken");
        try {
            backendClient.deletePatient(id, accessToken);
            redirectAttributes.addFlashAttribute("successMessage", "Clinical patient record deleted successfully.");
        } catch (Exception e) {
            String errorMsg = normalizeErrorMessage(e.getMessage(), "Unable to delete patient. Please try again later.");
            redirectAttributes.addFlashAttribute("errorMessage", errorMsg);
        }
        return "redirect:/patients";
    }

    @PostMapping("/{id}/restore")
    public String restorePatient(
            @PathVariable("id") String id,
            HttpSession session,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        if (session.getAttribute("authenticated") == null) {
            return "redirect:/login";
        }

        String accessToken = (String) session.getAttribute("accessToken");
        try {
            backendClient.restorePatient(id, accessToken);
            redirectAttributes.addFlashAttribute("successMessage", "Clinical patient record restored successfully.");
        } catch (Exception e) {
            String errorMsg = normalizeErrorMessage(e.getMessage(), "Unable to restore patient. Please try again later.");
            redirectAttributes.addFlashAttribute("errorMessage", errorMsg);
        }
        return "redirect:/patients";
    }
}
