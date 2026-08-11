package com.canineai.webapp.controller;

import com.canineai.webapp.client.BackendClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class NotificationController {

    private final BackendClient backendClient;

    @GetMapping("/notifications")
    public String viewNotifications(HttpSession session, Model model) {
        if (session.getAttribute("authenticated") == null) {
            return "redirect:/login";
        }
        
        model.addAttribute("notifications", List.of());
        model.addAttribute("unreadCount", 0);
        model.addAttribute("notificationsUnavailable", true);
        return "notifications";
    }

    @GetMapping({"/timeline", "/history"})
    public String viewTimeline(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "category", required = false) String category,
            HttpSession session,
            Model model) {
        
        if (session.getAttribute("authenticated") == null) {
            return "redirect:/login";
        }

        String token = (String) session.getAttribute("accessToken");
        List<Map<String, Object>> logs = List.of();
        try {
            logs = backendClient.getHistoryLogs(token);
        } catch (Exception ignored) {}

        model.addAttribute("timeline", logs);
        model.addAttribute("search", search);
        model.addAttribute("category", category);
        model.addAttribute("historyUnavailable", false);
        return "timeline";
    }

    @PostMapping("/notifications/read-all")
    public String markAllNotificationsRead() {
        return "redirect:/notifications?unavailable=true";
    }
}
