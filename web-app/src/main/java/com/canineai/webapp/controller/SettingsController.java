package com.canineai.webapp.controller;

import com.canineai.webapp.service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.canineai.webapp.client.BackendClient;
import com.canineai.webapp.dto.UserDto;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class SettingsController {

    private final EmailService emailService;
    private final BackendClient backendClient;

    public SettingsController(EmailService emailService, BackendClient backendClient) {
        this.emailService = emailService;
        this.backendClient = backendClient;
    }

    @GetMapping("/settings")
    public String showSettingsWorkspace(HttpSession session, Model model) {
        if (session.getAttribute("authenticated") == null) {
            return "redirect:/login";
        }
        
        String accessToken = (String) session.getAttribute("accessToken");
        UserDto user = backendClient.getCurrentUser(accessToken);
        
        Map<String, Object> settings = new HashMap<>();
        settings.put("fullName", user.getFullName() != null ? user.getFullName() : "");
        settings.put("email", user.getEmail() != null ? user.getEmail() : "");
        settings.put("username", user.getUsername() != null ? user.getUsername() : "");
        settings.put("phone", user.getPhone() != null ? user.getPhone() : "");
        settings.put("hospital", user.getHospital() != null ? user.getHospital() : "");
        settings.put("role", user.getRoleTitle() != null ? user.getRoleTitle() : "");
        settings.put("department", user.getDepartment() != null ? user.getDepartment() : "");
        settings.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toLocalDate().toString() : "");
        settings.put("enabled", user.isEnabled());
        settings.put("yearsOfExperience", user.getYearsOfExperience() != null ? user.getYearsOfExperience() : 0);
        settings.put("bloodGroup", user.getBloodGroup() != null ? user.getBloodGroup() : "");

        Boolean darkMode = (Boolean) session.getAttribute("darkMode");
        Boolean soundAlerts = (Boolean) session.getAttribute("soundAlerts");

        settings.put("darkMode", darkMode != null ? darkMode : false);
        settings.put("soundAlerts", soundAlerts != null ? soundAlerts : true);
        model.addAttribute("profile", settings);
        model.addAttribute("devices", List.of());
        
        // AI inference configuration
        model.addAttribute("aiEngine", null);
        model.addAttribute("modelVersion", null);
        model.addAttribute("inferenceTimeout", null);
        
        // Storage quotas
        model.addAttribute("storageUsedGb", null);
        model.addAttribute("maxStorageGb", null);
        return "settings";
    }

    @GetMapping("/profile")
    public String showProfileForm(HttpSession session, Model model) {
        if (session.getAttribute("authenticated") == null) {
            return "redirect:/login";
        }
        
        String accessToken = (String) session.getAttribute("accessToken");
        UserDto user = backendClient.getCurrentUser(accessToken);
        
        Map<String, Object> settings = new HashMap<>();
        settings.put("fullName", user.getFullName() != null ? user.getFullName() : "");
        settings.put("email", user.getEmail() != null ? user.getEmail() : "");
        settings.put("username", user.getUsername() != null ? user.getUsername() : "");
        settings.put("phone", user.getPhone() != null ? user.getPhone() : "");
        settings.put("hospital", user.getHospital() != null ? user.getHospital() : "");
        settings.put("role", user.getRoleTitle() != null ? user.getRoleTitle() : "");
        settings.put("department", user.getDepartment() != null ? user.getDepartment() : "");
        settings.put("medicalRegNo", user.getMedicalRegistrationNumber() != null ? user.getMedicalRegistrationNumber() : "");
        settings.put("yearsOfExperience", user.getYearsOfExperience() != null ? user.getYearsOfExperience() : 0);
        settings.put("bloodGroup", user.getBloodGroup() != null ? user.getBloodGroup() : "");
        settings.put("enabled", user.isEnabled());
        settings.put("roles", user.getRoles() != null ? String.join(", ", user.getRoles()) : "CLINICIAN");

        model.addAttribute("profile", settings);
        return "profile";
    }

    @PostMapping("/profile/save")
    public String saveProfileDetails(
            @RequestParam("fullName") String fullName,
            @RequestParam("phone") String phone,
            @RequestParam(value = "department", required = false) String department,
            @RequestParam(value = "hospital", required = false) String hospital,
            @RequestParam(value = "roleTitle", required = false) String roleTitle,
            @RequestParam(value = "medicalRegNo", required = false) String medicalRegNo,
            HttpSession session) {
        
        if (session.getAttribute("authenticated") == null) {
            return "redirect:/login";
        }
        
        String accessToken = (String) session.getAttribute("accessToken");
        
        UserDto profileReq = new UserDto();
        profileReq.setFullName(fullName);
        profileReq.setPhone(phone);
        profileReq.setDepartment(department);
        profileReq.setHospital(hospital);
        profileReq.setRoleTitle(roleTitle);
        profileReq.setMedicalRegistrationNumber(medicalRegNo);
        
        try {
            UserDto updatedUser = backendClient.updateProfile(profileReq, accessToken);
            
            session.setAttribute("doctorName", updatedUser.getFullName());
            session.setAttribute("organizationName", updatedUser.getHospital());
            
        } catch (Exception e) {
            log.error("Failed to save profile updates to backend database: {}", e.getMessage());
        }
        
        return "redirect:/settings";
    }

    @PostMapping("/settings/save")
    @ResponseBody
    public ResponseEntity<Void> saveSettings(
            @RequestParam(value = "darkMode", required = false) Boolean darkMode,
            @RequestParam(value = "soundAlerts", required = false) Boolean soundAlerts,
            @RequestParam(value = "temperature", required = false) Float temperature,
            HttpSession session) {

        if (darkMode != null) {
            session.setAttribute("darkMode", darkMode);
        }
        if (soundAlerts != null) {
            session.setAttribute("soundAlerts", soundAlerts);
        }
        if (temperature != null) {
            session.setAttribute("temperature", temperature);
        }

        return ResponseEntity.ok().build();
    }

    @PostMapping("/settings/clear-storage")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> clearStorage() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Storage management is unavailable because the backend has no storage-management API.");
        return ResponseEntity.status(501).body(response);
    }

    @PostMapping("/settings/email-digest")
    @ResponseBody
    public ResponseEntity<Void> emailWeeklyDigest() {
        return ResponseEntity.status(501).build();
    }
}
