package com.canineai.webapp.controller;

import com.canineai.webapp.client.BackendClient;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PatientControllerTest {

    @Test
    void shouldReturnFriendlyErrorMessageForMalformedBackendPayload() {
        BackendClient backendClient = mock(BackendClient.class);
        PatientController controller = new PatientController(backendClient);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("authenticated", true);
        session.setAttribute("accessToken", "token-123");
        session.setAttribute("organizationName", "Metro Dental Diagnostics");

        when(backendClient.getPatients(anyString())).thenReturn(List.of());
        when(backendClient.createPatient(anyMap(), eq("token-123")))
                .thenThrow(new RuntimeException("{\"message\":\"Invalid date format: 45.6618345}\\n\"}"));

        Model model = new ExtendedModelMap();
        String view = controller.savePatient(
                "",
                "Jane Doe",
                "34",
                "Female",
                "2001-01-01",
                "+1 555-0199",
                "jane@example.com",
                "Dr. Smith",
                "Routine visit",
                "A+",
                session,
                model
        );

        assertThat(view).isEqualTo("patient-form");
        assertThat(model.getAttribute("errorMessage")).isEqualTo("Invalid date of birth.");
    }

    @Test
    void patientDetailsUsesOwnerScopedPersistedStudiesAndReports() {
        BackendClient backendClient = mock(BackendClient.class);
        PatientController controller = new PatientController(backendClient);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("authenticated", true);
        session.setAttribute("accessToken", "doctor-token");

        Map<String, Object> patient = new java.util.HashMap<>(Map.of("id", "patient-1", "fullName", "Ada Patient"));
        Map<String, Object> study = Map.of("id", "study-1", "studyDescription", "CBCT", "status", "COMPLETED");
        Map<String, Object> ownReport = Map.of("id", "report-1", "studyId", "study-1", "status", "COMPLETED");
        Map<String, Object> otherReport = Map.of("id", "report-2", "studyId", "other-study", "status", "COMPLETED");
        when(backendClient.getPatient("patient-1", "doctor-token")).thenReturn(patient);
        when(backendClient.getPatientStudies("patient-1", "doctor-token")).thenReturn(List.of(study));
        when(backendClient.getReports("doctor-token")).thenReturn(List.of(ownReport, otherReport));

        Model model = new ExtendedModelMap();
        String view = controller.getPatientDetails("patient-1", session, model);

        assertThat(view).isEqualTo("patient-details");
        assertThat(model.getAttribute("studies")).isEqualTo(List.of(study));
        assertThat(model.getAttribute("reports")).isEqualTo(List.of(ownReport));
        assertThat(model.getAttribute("latestStudyId")).isEqualTo("study-1");
        verify(backendClient).getReports("doctor-token");
    }
}
