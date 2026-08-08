package com.canineai.webapp.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        String errorMessage = "An unexpected error occurred.";
        String errorCode = "500";

        // Log the complete exception instead of hiding it
        Throwable throwable = (Throwable) request.getAttribute("jakarta.servlet.error.exception");
        if (throwable != null) {
            log.error("Exception intercepted by CustomErrorController:", throwable);
            errorMessage = extractFriendlyMessage(throwable);
            
            String exClassName = throwable.getClass().getName();
            Throwable rc = throwable;
            while (rc.getCause() != null) rc = rc.getCause();
            String rcMsg = rc.getMessage() != null ? rc.getMessage() : "";
            
            if (exClassName.contains("ConnectException") || exClassName.contains("RetryableException") || 
                exClassName.contains("SocketTimeoutException") || rcMsg.contains("Connection refused") ||
                rcMsg.contains("connect timed out")) {
                errorCode = "AI_OFFLINE";
                errorMessage = "The Core CanineAI Inference Service or MySQL Database node is temporarily unreachable. Our clinical cloud synchronizer is automatically establishing a backup worker bridge.";
            }
        }

        if (status != null && !errorCode.equals("AI_OFFLINE")) {
            int statusCode = Integer.parseInt(status.toString());
            errorCode = String.valueOf(statusCode);
            if (statusCode == 404) {
                errorMessage = "The requested clinical web workspace route was not found in our dispatcher map.";
            } else if (statusCode == 403) {
                errorMessage = "You do not possess the required clinician credential roles to access this diagnostic workspace.";
            } else if (statusCode == 500 && throwable == null) {
                errorMessage = "We encountered a clinical pipeline dispatch timeout. Please retry the transaction.";
            }
        }

        model.addAttribute("errorCode", errorCode);
        model.addAttribute("errorMessage", errorMessage);
        return "error";
    }

    private String extractFriendlyMessage(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        String msg = cause.getMessage();
        if (msg == null) return "An unexpected error occurred.";
        
        // Parse JSON validation error body if present
        if (msg.trim().startsWith("{")) {
            try {
                java.util.regex.Pattern pMsg = java.util.regex.Pattern.compile("\"message\"\\s*:\\s*\"([^\"]+)\"");
                java.util.regex.Pattern pDet = java.util.regex.Pattern.compile("\"details\"\\s*:\\s*\"([^\"]+)\"");
                
                java.util.regex.Matcher mMsg = pMsg.matcher(msg);
                java.util.regex.Matcher mDet = pDet.matcher(msg);
                
                String mainMessage = mMsg.find() ? mMsg.group(1) : "";
                String details = mDet.find() ? mDet.group(1) : "";
                
                if (!details.isEmpty()) {
                    return mainMessage + ": " + details;
                } else if (!mainMessage.isEmpty()) {
                    return mainMessage;
                }
            } catch (Exception e) {
                // Fallback to raw message
            }
        }
        return msg;
    }
}
