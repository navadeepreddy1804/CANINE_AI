package com.canineai.webapp.client;

import com.canineai.webapp.dto.ApiResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BackendClientTest {

    @Test
    void shouldNormalizeUnexpectedErrorPayloadsToUserFriendlyMessage() {
        BackendClient client = new BackendClient("http://localhost:8080/api/v1");

        String message = client.extractErrorMessage("{\"message\":\"45.6618345}\\n\"}", "An unexpected error occurred. Please try again.");

        assertThat(message).isEqualTo("An unexpected error occurred. Please try again.");
    }

    @Test
    void shouldNormalizeDateParsingErrorsToFriendlyMessage() {
        BackendClient client = new BackendClient("http://localhost:8080/api/v1");

        String message = client.extractErrorMessage("{\"message\":\"Invalid date format: 45.6618345}\\n\"}", "An unexpected error occurred. Please try again.");

        assertThat(message).isEqualTo("Invalid date of birth.");
    }

    @Test
    void getReports_usesPersistedReportsEndpointAndForwardsBearerToken() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        BackendClient client = new BackendClient("https://backend.example/api/v1", restTemplate);
        ApiResponse<List<Map<String, Object>>> body = success(List.of(Map.of("id", "report-1")));
        stubJsonGet(restTemplate, "https://backend.example/api/v1/reports", body);

        List<Map<String, Object>> reports = client.getReports("access-token");

        assertThat(reports).containsExactly(Map.of("id", "report-1"));
        assertBearerHeader(restTemplate, "https://backend.example/api/v1/reports");
    }

    @Test
    void getReport_usesReportIdRatherThanPatientId() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        BackendClient client = new BackendClient("https://backend.example/api/v1", restTemplate);
        ApiResponse<Map<String, Object>> body = success(Map.of("id", "report-uuid"));
        stubJsonGet(restTemplate, "https://backend.example/api/v1/reports/report-uuid", body);

        Map<String, Object> report = client.getReport("report-uuid", "access-token");

        assertThat(report).containsEntry("id", "report-uuid");
        assertBearerHeader(restTemplate, "https://backend.example/api/v1/reports/report-uuid");
    }

    @Test
    void downloadReportPdf_usesPersistedPdfEndpointAndForwardsBearerToken() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        BackendClient client = new BackendClient("https://backend.example/api/v1", restTemplate);
        byte[] expected = {1, 2, 3};
        when(restTemplate.exchange(
                eq("https://backend.example/api/v1/reports/report-uuid/pdf"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(byte[].class)))
                .thenReturn(ResponseEntity.ok(expected));

        assertThat(client.downloadReportPdf("report-uuid", "access-token")).containsExactly(expected);

        assertBearerHeaderForBytes(restTemplate, "https://backend.example/api/v1/reports/report-uuid/pdf");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void stubJsonGet(RestTemplate restTemplate, String url, ApiResponse body) {
        when(restTemplate.exchange(eq(url), eq(HttpMethod.GET), any(HttpEntity.class),
                any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(body));
    }

    private void assertBearerHeader(RestTemplate restTemplate, String url) {
        ArgumentCaptor<HttpEntity<?>> request = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(eq(url), eq(HttpMethod.GET), request.capture(), any(ParameterizedTypeReference.class));
        assertThat(request.getValue().getHeaders().getFirst("Authorization")).isEqualTo("Bearer access-token");
    }

    private void assertBearerHeaderForBytes(RestTemplate restTemplate, String url) {
        ArgumentCaptor<HttpEntity<?>> request = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(eq(url), eq(HttpMethod.GET), request.capture(), eq(byte[].class));
        assertThat(request.getValue().getHeaders().getFirst("Authorization")).isEqualTo("Bearer access-token");
    }

    private <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(true);
        response.setData(data);
        return response;
    }
}
