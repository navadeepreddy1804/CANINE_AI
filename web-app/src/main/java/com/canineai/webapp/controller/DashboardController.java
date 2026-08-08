package com.canineai.webapp.controller;

import com.canineai.webapp.client.BackendClient;
import com.canineai.webapp.dto.UserDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Controller
public class DashboardController {

    private final BackendClient backendClient;

    public DashboardController(BackendClient backendClient) {
        this.backendClient = backendClient;
    }

    private String formatDoctorName(String name) {
        if (name == null || name.isBlank()) return "";
        String trimmed = name.trim();
        if (trimmed.toLowerCase().startsWith("dr.")) {
            String stripped = trimmed.substring(3).trim();
            return "Dr. " + stripped;
        }
        return "Dr. " + trimmed;
    }

    @GetMapping({"/dashboard", "/home"})
    public String showDashboard(HttpSession session, Model model) {
        boolean isAuthenticated = session.getAttribute("authenticated") != null;

        model.addAttribute("authenticated", isAuthenticated);

        // Format today's date dynamically
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
        String formattedDate = LocalDateTime.now().format(formatter);
        model.addAttribute("todayDate", formattedDate);

        if (!isAuthenticated) {
            // Guest showcase view
            model.addAttribute("aiModelName", "ToothSeg v1.2");
            model.addAttribute("doctorName", "Guest Clinician");
            model.addAttribute("organizationName", "CanineAI Platform Demonstration");
            model.addAttribute("totalPatients", 0);
            model.addAttribute("todayUploads", 0);
            model.addAttribute("pendingAnalyses", 0);
            model.addAttribute("completedAnalyses", 0);
            model.addAttribute("reportsGenerated", 0);
            model.addAttribute("utilizationPercentage", 0);
            model.addAttribute("storageUsedGb", "0.00");
            model.addAttribute("maxStorageGb", 100.0f);
            model.addAttribute("unreadCount", 0);
            model.addAttribute("recentActivities", List.of());
            model.addAttribute("recentPatients", List.of());
            return "dashboard";
        }

        // Authenticated view
        model.addAttribute("aiModelName", "ToothSeg v1.2");
        String accessToken = (String) session.getAttribute("accessToken");
        
        UserDto currentUser;
        try {
            currentUser = backendClient.getCurrentUser(accessToken);
        } catch (Exception e) {
            log.warn("Active session token invalid or expired. Redirecting to login: {}", e.getMessage());
            session.invalidate();
            return "redirect:/login";
        }
        String doctorName = currentUser.getFullName() != null ? formatDoctorName(currentUser.getFullName()) : "Clinician";
        String hospital = currentUser.getHospital() != null ? currentUser.getHospital() : "Hospital";

        // Save in session for fast lookup
        session.setAttribute("doctorName", doctorName);
        session.setAttribute("organizationName", hospital);

        model.addAttribute("doctorName", doctorName);
        model.addAttribute("organizationName", hospital);

        // Fetch patients from real backend database
        List<Map<String, Object>> dbPatients = backendClient.getPatients(accessToken);
        
        // Filter patients dynamically by current doctor (case-insensitive, normalized)
        if (dbPatients != null) {
            String normalizedDoc = doctorName.toLowerCase().replace("dr.", "").trim();
            dbPatients = dbPatients.stream().filter(p -> {
                String orth = p.get("orthodontist") != null ? p.get("orthodontist").toString() : "";
                String normalizedOrth = orth.toLowerCase().replace("dr.", "").trim();
                return normalizedOrth.equals(normalizedDoc);
            }).collect(Collectors.toList());
        } else {
            dbPatients = List.of();
        }

        int totalPatients = dbPatients.size();
        int reportsGenerated = backendClient.getReports(accessToken).size();

        // A completed persisted report is the only aggregate analysis result currently
        // exposed by the backend. The backend has no global pending-job or storage API.
        long completedAnalyses = reportsGenerated;

        model.addAttribute("totalPatients", totalPatients);
        model.addAttribute("todayUploads", 0);
        model.addAttribute("pendingAnalyses", 0);
        model.addAttribute("completedAnalyses", completedAnalyses);
        model.addAttribute("reportsGenerated", reportsGenerated);

        model.addAttribute("utilizationPercentage", null);
        model.addAttribute("storageUsedGb", null);
        model.addAttribute("maxStorageGb", null);

        // Bind Notifications count
        model.addAttribute("unreadCount", 0);

        // The backend does not expose an activity-feed endpoint.
        model.addAttribute("recentActivities", List.of());
        model.addAttribute("recentPatients", dbPatients.stream().limit(5).collect(Collectors.toList()));

        return "dashboard";
    }
}
