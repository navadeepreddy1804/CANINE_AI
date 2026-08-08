package com.canineai.webapp.controller;

import com.canineai.webapp.client.BackendClient;
import com.canineai.webapp.dto.LoginRequest;
import com.canineai.webapp.dto.LoginResponse;
import com.canineai.webapp.dto.UserDto;
import com.canineai.webapp.service.EmailService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorkflowValidationTest {

    @Test
    void shouldValidateLoginDashboardAndUploadWorkflow() {
        BackendClient backendClient = mock(BackendClient.class);
        EmailService emailService = mock(EmailService.class);
        LoginController loginController = new LoginController(backendClient, emailService);
        DashboardController dashboardController = new DashboardController(backendClient);
        UploadController uploadController = new UploadController(backendClient);

        MockHttpSession session = new MockHttpSession();
        MockHttpServletResponse response = new MockHttpServletResponse();

        UserDto user = new UserDto();
        user.setEmail("doctor@example.com");
        user.setFullName("Jane Doe");
        user.setHospital("Metro Dental Diagnostics");
        user.setRoleTitle("Orthodontist");
        user.setPhone("+1 555-0123");

        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setAccessToken("access-token");
        loginResponse.setRefreshToken("refresh-token");
        loginResponse.setUser(user);

        when(backendClient.login(any(LoginRequest.class))).thenReturn(loginResponse);
        when(backendClient.getCurrentUser(anyString())).thenReturn(user);
        when(backendClient.getPatients(anyString())).thenReturn(List.of(Map.of(
                "id", "pt-1",
                "fullName", "Jane Doe",
                "orthodontist", "Dr. Jane Doe"
        )));
        when(backendClient.getPatient(eq("pt-1"), anyString())).thenReturn(Map.of(
                "id", "pt-1",
                "fullName", "Jane Doe"
        ));

        String loginView = loginController.handleLogin(
                "doctor@example.com",
                "s3cr3t!",
                false,
                session,
                response
        );

        assertThat(loginView).isEqualTo("redirect:/dashboard");
        assertThat(session.getAttribute("authenticated")).isEqualTo(true);
        assertThat(session.getAttribute("doctorName")).isEqualTo("Dr. Jane Doe");

        Model model = new ExtendedModelMap();
        String dashboardView = dashboardController.showDashboard(session, model);

        assertThat(dashboardView).isEqualTo("dashboard");
        assertThat(model.getAttribute("doctorName")).isEqualTo("Dr. Jane Doe");
        assertThat(model.getAttribute("totalPatients")).isEqualTo(1);

        String uploadView = uploadController.showUploadWorkspace("pt-1", session, model);
        assertThat(uploadView).isEqualTo("upload");
        assertThat(model.getAttribute("patients")).isNotNull();

    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldValidateCompleteAiAnalysisAndReportWorkflow() {
        BackendClient backendClient = mock(BackendClient.class);
        AnalysisController analysisController = new AnalysisController(backendClient);
        ReportController reportController = new ReportController(backendClient);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("authenticated", true);
        session.setAttribute("accessToken", "access-token");

        // 1. Start AI Job on Study
        when(backendClient.submitAiJob("study-123", "access-token"))
                .thenReturn(Map.of("jobId", "job-999", "state", "QUEUED", "progressPercentage", 0));

        ResponseEntity<Map<String, Object>> submitRes = analysisController.submitAnalysisJob("study-123", session);
        assertThat(submitRes.getBody()).containsEntry("jobId", "job-999");
        assertThat(submitRes.getBody()).containsEntry("state", "QUEUED");

        // 2. Poll progress until COMPLETED without page refresh
        when(backendClient.getAiJobProgress("job-999", "access-token"))
                .thenReturn(new java.util.HashMap<>(Map.of("jobId", "job-999", "state", "COMPLETED", "progressPercentage", 100)));
        when(backendClient.getAiJob("job-999", "access-token"))
                .thenReturn(Map.of("jobId", "job-999", "state", "COMPLETED", "resultJson", "{\"canineFdi\": 13, \"prediction\": \"IMPACTED\"}"));

        ResponseEntity<Map<String, Object>> pollRes = analysisController.getAnalysisJob("job-999", session);
        assertThat(pollRes.getBody()).containsEntry("state", "COMPLETED");
        assertThat(pollRes.getBody()).containsKey("resultJson");
        assertThat(pollRes.getBody().get("resultJson")).isEqualTo("{\"canineFdi\": 13, \"prediction\": \"IMPACTED\"}");

        // 3. Verify Report generation and retrieval by studyId
        when(backendClient.getReportByStudyId("study-123", "access-token"))
                .thenReturn(Map.of("id", "rep-500", "studyId", "study-123", "status", "COMPLETED", "prediction", "IMPACTED"));

        ResponseEntity<?> reportRes = reportController.getReportByStudyId("study-123", session);
        assertThat(reportRes.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(((Map<String, Object>) reportRes.getBody()).get("data")).isNotNull();
    }
}
