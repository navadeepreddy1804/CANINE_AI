package com.canineai.webapp.controller;

import com.canineai.webapp.client.BackendClient;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportControllerTest {

    @Test
    void listReports_loadsPersistedReportsForAuthenticatedSession() {
        BackendClient client = mock(BackendClient.class);
        ReportController controller = new ReportController(client);
        MockHttpSession session = authenticatedSession();
        Map<String, Object> report = Map.of("id", "report-uuid", "studyId", "study-uuid", "status", "COMPLETED");
        when(client.getReports("token-123")).thenReturn(List.of(report));

        ExtendedModelMap model = new ExtendedModelMap();
        String view = controller.listReports(session, model);

        assertThat(view).isEqualTo("reports");
        assertThat(model.getAttribute("reports")).isEqualTo(List.of(report));
        verify(client).getReports("token-123");
    }

    @Test
    void reportDetails_usesReportIdAndDisplaysPersistedReport() {
        BackendClient client = mock(BackendClient.class);
        ReportController controller = new ReportController(client);
        MockHttpSession session = authenticatedSession();
        Map<String, Object> report = Map.of("id", "report-uuid", "reportMarkdown", "Stored report");
        when(client.getReport("report-uuid", "token-123")).thenReturn(report);

        ExtendedModelMap model = new ExtendedModelMap();
        String view = controller.reportDetails("report-uuid", session, model);

        assertThat(view).isEqualTo("report-details");
        assertThat(model.getAttribute("report")).isEqualTo(report);
        verify(client).getReport("report-uuid", "token-123");
    }

    @Test
    void downloadReportPdf_usesReportIdAndPersistedPdfClientMethod() {
        BackendClient client = mock(BackendClient.class);
        ReportController controller = new ReportController(client);
        MockHttpSession session = authenticatedSession();
        byte[] pdf = {1, 2, 3};
        when(client.downloadReportPdf("report-uuid", "token-123")).thenReturn(pdf);

        var response = controller.downloadReportPdf("report-uuid", session);

        assertThat(response.getBody()).containsExactly(pdf);
        verify(client).downloadReportPdf("report-uuid", "token-123");
    }

    @Test
    void reportRoutes_redirectAnonymousUsersToLogin() {
        ReportController controller = new ReportController(mock(BackendClient.class));
        MockHttpSession session = new MockHttpSession();

        assertThat(controller.listReports(session, new ExtendedModelMap())).isEqualTo("redirect:/login");
        assertThat(controller.reportDetails("report-uuid", session, new ExtendedModelMap())).isEqualTo("redirect:/login");
        assertThat(controller.downloadReportPdf("report-uuid", session).getStatusCode().value()).isEqualTo(401);
    }

    private MockHttpSession authenticatedSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("authenticated", true);
        session.setAttribute("accessToken", "token-123");
        return session;
    }
}
