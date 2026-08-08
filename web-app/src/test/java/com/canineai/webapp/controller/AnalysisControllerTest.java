package com.canineai.webapp.controller;

import com.canineai.webapp.client.BackendClient;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnalysisControllerTest {

    @Test
    void shouldUseRequestedStudyWhenPresent() {
        BackendClient backendClient = mock(BackendClient.class);
        AnalysisController controller = new AnalysisController(backendClient);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("authenticated", true);
        session.setAttribute("accessToken", "token-123");

        Map<String, Object> patient = Map.of(
                "id", "pt-1",
                "fullName", "Jane Doe",
                "hospitalPatientId", "PT-1001"
        );

        List<Map<String, Object>> studies = List.of(
                Map.of("id", "study-1", "sliceCount", 12, "studyDescription", "First study"),
                Map.of("id", "study-2", "sliceCount", 8, "studyDescription", "Second study")
        );

        when(backendClient.getPatients(anyString())).thenReturn(List.of(patient));
        when(backendClient.getPatientStudies("pt-1", "token-123")).thenReturn(studies);

        Model model = new ExtendedModelMap();
        String view = controller.showAnalysisWorkspace("pt-1", "study-2", session, model);

        assertThat(view).isEqualTo("analysis");
        assertThat(model.getAttribute("studyId")).isEqualTo("study-2");
        assertThat(model.getAttribute("study")).isInstanceOf(Map.class);
        assertThat(((Map<String, Object>) model.getAttribute("study")).get("id")).isEqualTo("study-2");
    }

    @Test
    void shouldIsolateMultipleStudiesAcrossPatientsAndStudies() {
        BackendClient backendClient = mock(BackendClient.class);
        AnalysisController controller = new AnalysisController(backendClient);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("authenticated", true);
        session.setAttribute("accessToken", "token-123");

        Map<String, Object> patientA = Map.of("id", "pt-A", "fullName", "Patient A");
        Map<String, Object> patientB = Map.of("id", "pt-B", "fullName", "Patient B");

        List<Map<String, Object>> studiesA = List.of(
                Map.of("id", "study-A1", "studyDescription", "Scan A1"),
                Map.of("id", "study-A2", "studyDescription", "Scan A2")
        );
        List<Map<String, Object>> studiesB = List.of(
                Map.of("id", "study-B1", "studyDescription", "Scan B1")
        );

        when(backendClient.getPatients("token-123")).thenReturn(List.of(patientA, patientB));
        when(backendClient.getPatientStudies("pt-A", "token-123")).thenReturn(studiesA);
        when(backendClient.getPatientStudies("pt-B", "token-123")).thenReturn(studiesB);

        // When requesting study-B1 without explicit patientId, controller resolves patientB
        Model modelB = new ExtendedModelMap();
        String viewB = controller.showAnalysisWorkspace(null, "study-B1", session, modelB);
        assertThat(viewB).isEqualTo("analysis");
        assertThat(modelB.getAttribute("studyId")).isEqualTo("study-B1");
        assertThat(((Map<String, Object>) modelB.getAttribute("patient")).get("id")).isEqualTo("pt-B");

        // When requesting study-A2 explicitly, controller isolates study-A2
        Model modelA = new ExtendedModelMap();
        String viewA = controller.showAnalysisWorkspace("pt-A", "study-A2", session, modelA);
        assertThat(viewA).isEqualTo("analysis");
        assertThat(modelA.getAttribute("studyId")).isEqualTo("study-A2");
        assertThat(((Map<String, Object>) modelA.getAttribute("patient")).get("id")).isEqualTo("pt-A");
    }

    @Test
    void shouldIncludeResultJsonWhenJobStateIsCompleted() {
        BackendClient backendClient = mock(BackendClient.class);
        AnalysisController controller = new AnalysisController(backendClient);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("authenticated", true);
        session.setAttribute("accessToken", "token-123");

        Map<String, Object> statusResponse = new java.util.HashMap<>();
        statusResponse.put("jobId", "job-123");
        statusResponse.put("state", "COMPLETED");
        statusResponse.put("progressPercentage", 100);

        when(backendClient.getAiJobProgress("job-123", "token-123")).thenReturn(statusResponse);

        Map<String, Object> detailsResponse = new java.util.HashMap<>();
        detailsResponse.put("jobId", "job-123");
        detailsResponse.put("state", "COMPLETED");
        detailsResponse.put("resultJson", "{\"canineFdi\": 13}");
        when(backendClient.getAiJob("job-123", "token-123")).thenReturn(detailsResponse);

        // When the controller calls getAnalysisJob, it must verify if state is COMPLETED and include the result
        org.springframework.http.ResponseEntity<Map<String, Object>> response = controller.getAnalysisJob("job-123", session);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKey("resultJson");
        assertThat(response.getBody().get("resultJson")).isEqualTo("{\"canineFdi\": 13}");
    }
}

