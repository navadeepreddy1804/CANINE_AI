package com.canineai.webapp.client;

import com.canineai.webapp.dto.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Component
public class BackendClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    @Autowired
    public BackendClient(
            @Value("${canineai.backend.base-url:http://localhost:8080/api/v1}") String baseUrl,
            RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    // Package-visible constructor retained solely for focused client unit tests.
    BackendClient(String baseUrl) {
        this(baseUrl, new RestTemplate());
    }

    public UserDto register(RegisterRequest request) {
        String url = baseUrl + "/auth/register";
        log.info("Sending signup registration request to backend: {}", url);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<RegisterRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<ApiResponse<UserDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<ApiResponse<UserDto>>() {}
            );

            if (response.getBody() != null && response.getBody().isSuccess()) {
                return response.getBody().getData();
            } else {
                String msg = response.getBody() != null ? response.getBody().getMessage() : "Unknown backend error";
                throw new RuntimeException(msg);
            }
        } catch (HttpStatusCodeException ex) {
            String body = ex.getResponseBodyAsString();
            log.error("Register request failed: backend returned an error");
            throw new RuntimeException(extractErrorMessage(body, "Signup failed"));
        } catch (Exception e) {
            log.error("Register request failed: {}", e.getMessage());
            throw new RuntimeException("Signup failed: " + e.getMessage());
        }
    }

    public LoginResponse login(LoginRequest request) {
        String url = baseUrl + "/auth/login";
        log.info("Sending authentication request to backend: {}", url);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<LoginRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<ApiResponse<LoginResponse>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<ApiResponse<LoginResponse>>() {}
            );

            if (response.getBody() != null && response.getBody().isSuccess()) {
                return response.getBody().getData();
            } else {
                String msg = response.getBody() != null ? response.getBody().getMessage() : "Invalid credentials";
                throw new RuntimeException(msg);
            }
        } catch (HttpStatusCodeException ex) {
            String body = ex.getResponseBodyAsString();
            log.error("Login request failed: backend returned an error");
            throw new RuntimeException(extractErrorMessage(body, "Invalid email or password"));
        } catch (Exception e) {
            log.error("Login request failed: {}", e.getMessage());
            throw new RuntimeException("Login failed: " + e.getMessage());
        }
    }

    public LoginResponse googleLogin(String idToken) {
        String url = baseUrl + "/auth/google";
        log.info("Sending Google authentication request to backend: {}", url);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, String> body = new HashMap<>();
            body.put("idToken", idToken);
            
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<ApiResponse<LoginResponse>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<ApiResponse<LoginResponse>>() {}
            );

            if (response.getBody() != null && response.getBody().isSuccess()) {
                return response.getBody().getData();
            } else {
                String msg = response.getBody() != null ? response.getBody().getMessage() : "Google authentication failed";
                throw new RuntimeException(msg);
            }
        } catch (HttpStatusCodeException ex) {
            String bodyString = ex.getResponseBodyAsString();
            log.error("Google login request failed: backend returned an error");
            throw new RuntimeException(extractErrorMessage(bodyString, "Google authentication failed"));
        } catch (Exception e) {
            log.error("Google login request failed: {}", e.getMessage());
            throw new RuntimeException("Google Login failed: " + e.getMessage());
        }
    }
    public String getSecurityQuestion(String email) {
        String url = baseUrl + "/auth/security-question?email=" + email;
        log.info("Fetching security question from backend: {}", url);
        try {
            ResponseEntity<ApiResponse<String>> response = restTemplate.exchange(
                    url, HttpMethod.GET, null, new ParameterizedTypeReference<ApiResponse<String>>() {}
            );
            if (response.getBody() != null && response.getBody().isSuccess()) {
                return response.getBody().getData();
            } else {
                throw new RuntimeException("Failed to fetch security question");
            }
        } catch (HttpStatusCodeException ex) {
            throw new RuntimeException(extractErrorMessage(ex.getResponseBodyAsString(), "Failed to retrieve security question"));
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve security question: " + e.getMessage());
        }
    }

    public String forgotPassword(ForgotPasswordRequest request) {
        String url = baseUrl + "/auth/forgot-password";
        log.info("Sending forgot password request to backend: {}", url);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ForgotPasswordRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<ApiResponse<String>> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, new ParameterizedTypeReference<ApiResponse<String>>() {}
            );

            if (response.getBody() != null && response.getBody().isSuccess()) {
                return response.getBody().getData(); // Returns the reset token
            } else {
                throw new RuntimeException("Forgot password failed");
            }
        } catch (HttpStatusCodeException ex) {
            throw new RuntimeException(extractErrorMessage(ex.getResponseBodyAsString(), "Forgot password failed"));
        } catch (Exception e) {
            throw new RuntimeException("Forgot password failed: " + e.getMessage());
        }
    }

    public void resetPassword(ResetPasswordRequest request) {
        String url = baseUrl + "/auth/reset-password";
        log.info("Sending reset password request to backend: {}", url);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ResetPasswordRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<ApiResponse<Void>> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, new ParameterizedTypeReference<ApiResponse<Void>>() {}
            );

            if (response.getBody() == null || !response.getBody().isSuccess()) {
                throw new RuntimeException("Reset password failed");
            }
        } catch (HttpStatusCodeException ex) {
            throw new RuntimeException(extractErrorMessage(ex.getResponseBodyAsString(), "Reset password failed"));
        } catch (Exception e) {
            throw new RuntimeException("Reset password failed: " + e.getMessage());
        }
    }

    public void logout(String refreshToken, String bearerToken) {
        String url = baseUrl + "/auth/logout?refreshToken=" + refreshToken;
        log.info("Sending logout request to backend: {}", url);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + bearerToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<ApiResponse<Void>>() {}
            );
        } catch (Exception e) {
            log.warn("Logout request failed: {}", e.getMessage());
        }
    }

    public UserDto getCurrentUser(String bearerToken) {
        String url = baseUrl + "/auth/me";
        log.info("Fetching profile details from backend: {}", url);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + bearerToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<ApiResponse<UserDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<ApiResponse<UserDto>>() {}
            );

            if (response.getBody() != null && response.getBody().isSuccess()) {
                return response.getBody().getData();
            } else {
                throw new RuntimeException("Failed to fetch user session");
            }
        } catch (Exception e) {
            log.error("Get current user request failed: {}", e.getMessage());
            throw new RuntimeException("Profile retrieval failed: " + e.getMessage());
        }
    }

    public Map<String, Object> refreshToken(String refreshToken) {
        String url = baseUrl + "/auth/refresh";
        log.info("Sending refresh token request to backend: {}", url);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> body = new HashMap<>();
            body.put("refreshToken", refreshToken);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<ApiResponse<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {}
            );

            if (response.getBody() != null && response.getBody().isSuccess()) {
                return response.getBody().getData();
            } else {
                throw new RuntimeException("Refresh token validation failed");
            }
        } catch (Exception e) {
            log.error("Token refresh request failed: {}", e.getMessage());
            throw new RuntimeException("Refresh token failed: " + e.getMessage());
        }
    }

    public List<Map<String, Object>> getPatients(String search, String gender, String status, int page, int size, String accessToken) {
        StringBuilder urlBuilder = new StringBuilder(baseUrl);
        if (search != null && !search.isBlank()) {
            urlBuilder.append("/patients/search?query=").append(search).append("&page=").append(page).append("&size=").append(size);
        } else {
            urlBuilder.append("/patients?page=").append(page).append("&size=").append(size);
            if (gender != null && !gender.isBlank()) urlBuilder.append("&gender=").append(gender.toUpperCase());
            if (status != null && !status.isBlank()) urlBuilder.append("&status=").append(status.toUpperCase());
        }
        
        String url = urlBuilder.toString();
        log.info("Fetching patients from backend: {}", url);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<ApiResponse<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {}
            );

            if (response.getBody() != null && response.getBody().isSuccess()) {
                Map<String, Object> pagedData = response.getBody().getData();
                if (pagedData != null && pagedData.containsKey("content")) {
                    return (List<Map<String, Object>>) pagedData.get("content");
                }
            }
            return new ArrayList<>();
        } catch (org.springframework.web.client.HttpStatusCodeException ex) {
            String body = ex.getResponseBodyAsString();
            log.error("Get patients request failed: backend returned an error");
            throw new RuntimeException(extractErrorMessage(body, "Failed to retrieve patients"));
        } catch (Exception e) {
            log.error("Get patients request failed: {}", e.getMessage());
            throw new RuntimeException("Failed to retrieve patients: " + e.getMessage(), e);
        }
    }

    public List<Map<String, Object>> getPatients(String accessToken) {
        return getPatients(null, null, null, 0, 1000, accessToken);
    }

    public Map<String, Object> getPatient(String id, String accessToken) {
        String url = baseUrl + "/patients/" + id;
        log.info("Fetching single patient from backend: {}", url);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<ApiResponse<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {}
            );

            if (response.getBody() != null && response.getBody().isSuccess()) {
                return response.getBody().getData();
            }
            return null;
        } catch (org.springframework.web.client.HttpStatusCodeException ex) {
            String body = ex.getResponseBodyAsString();
            log.error("Get patient request failed: backend returned an error");
            throw new RuntimeException(extractErrorMessage(body, "Failed to retrieve patient profile"));
        } catch (Exception e) {
            log.error("Get patient request failed: {}", e.getMessage());
            throw new RuntimeException("Failed to retrieve patient profile: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> createPatient(Map<String, Object> patientReq, String accessToken) {
        String url = baseUrl + "/patients";
        log.info("Creating patient on backend: {}", url);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + accessToken);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(patientReq, headers);

            ResponseEntity<ApiResponse<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {}
            );

            if (response.getBody() != null && response.getBody().isSuccess()) {
                return response.getBody().getData();
            }
            throw new RuntimeException(extractErrorMessage(response.getBody() != null ? response.getBody().getMessage() : null, "An unexpected error occurred. Please try again."));
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            String errorBody = e.getResponseBodyAsString();
            log.error("Create patient request failed: backend returned an error");
            throw new RuntimeException(extractErrorMessage(errorBody, "An unexpected error occurred. Please try again."));
        } catch (Exception e) {
            log.error("Create patient request failed: {}", e.getMessage());
            throw new RuntimeException("Create patient failed: " + e.getMessage());
        }
    }

    public Map<String, Object> updatePatient(String id, Map<String, Object> patientReq, String accessToken) {
        String url = baseUrl + "/patients/" + id;
        log.info("Updating patient on backend: {}", url);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + accessToken);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(patientReq, headers);

            ResponseEntity<ApiResponse<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    entity,
                    new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {}
            );

            if (response.getBody() != null && response.getBody().isSuccess()) {
                return response.getBody().getData();
            }
            throw new RuntimeException(extractErrorMessage(response.getBody() != null ? response.getBody().getMessage() : null, "An unexpected error occurred. Please try again."));
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            String errorBody = e.getResponseBodyAsString();
            log.error("Update patient request failed: backend returned an error");
            throw new RuntimeException(extractErrorMessage(errorBody, "An unexpected error occurred. Please try again."));
        } catch (Exception e) {
            log.error("Update patient request failed: {}", e.getMessage());
            throw new RuntimeException("Update patient failed: " + e.getMessage());
        }
    }

    public void deletePatient(String id, String accessToken) {
        String url = baseUrl + "/patients/" + id;
        log.info("Deleting patient on backend: {}", url);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    entity,
                    new ParameterizedTypeReference<ApiResponse<Void>>() {}
            );
        } catch (Exception e) {
            log.error("Delete patient request failed: {}", e.getMessage());
            throw new RuntimeException("Delete patient failed: " + e.getMessage());
        }
    }

    public void restorePatient(String id, String accessToken) {
        String url = baseUrl + "/patients/" + id + "/restore";
        log.info("Restoring patient on backend: {}", url);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {}
            );
        } catch (Exception e) {
            log.error("Restore patient request failed: {}", e.getMessage());
            throw new RuntimeException("Restore patient failed: " + e.getMessage());
        }
    }

    public byte[] getPreviewImage(String studyId, String type, String accessToken) {
        String url = baseUrl + "/studies/" + studyId + "/previews/" + type;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Arrays.asList(MediaType.IMAGE_PNG, MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL));
            headers.set("Authorization", "Bearer " + accessToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<byte[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    byte[].class
            );

            log.info("Fetched preview from backend: url={}, status={}, contentType={}, bytes={}",
                    url,
                    response.getStatusCodeValue(),
                    response.getHeaders().getContentType(),
                    response.getBody() != null ? response.getBody().length : 0);
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to fetch study preview from backend: id={}, type={}, error={}", studyId, type, e.getMessage());
            return null;
        }
    }

    public byte[] getIndexedPreviewImage(String studyId, String type, int index, String accessToken) {
        String url = baseUrl + "/studies/" + studyId + "/previews/" + type + "/" + index;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Arrays.asList(MediaType.IMAGE_PNG, MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL));
            headers.set("Authorization", "Bearer " + accessToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<byte[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    byte[].class
            );

            log.info("Fetched indexed preview from backend: url={}, status={}, contentType={}, bytes={}",
                    url,
                    response.getStatusCodeValue(),
                    response.getHeaders().getContentType(),
                    response.getBody() != null ? response.getBody().length : 0);
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to fetch study indexed preview from backend: id={}, type={}, index={}, error={}", studyId, type, index, e.getMessage());
            return null;
        }
    }

    public List<String> getDicomList(String studyId, String accessToken) {
        String url = baseUrl + "/studies/" + studyId + "/dicom/list";
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<List<String>> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, new ParameterizedTypeReference<List<String>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to fetch DICOM list from backend: id={}, error={}", studyId, e.getMessage());
            return new ArrayList<>();
        }
    }

    public byte[] getDicomFile(String studyId, String filename, String accessToken) {
        String url = baseUrl + "/studies/" + studyId + "/dicom/" + filename;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL));
            headers.set("Authorization", "Bearer " + accessToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<byte[]> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, byte[].class
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to fetch DICOM file from backend: id={}, filename={}, error={}", studyId, filename, e.getMessage());
            return null;
        }
    }

    public List<Map<String, Object>> getPatientStudies(String patientId, String accessToken) {
        String url = baseUrl + "/patients/" + patientId + "/studies";
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<ApiResponse<List<Map<String, Object>>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<ApiResponse<List<Map<String, Object>>>>() {}
            );

            if (response.getBody() != null && response.getBody().isSuccess()) {
                return response.getBody().getData();
            }
            return new ArrayList<>();
        } catch (org.springframework.web.client.HttpStatusCodeException ex) {
            String body = ex.getResponseBodyAsString();
            log.error("Failed to fetch studies for patient: patientId={}", patientId);
            throw new RuntimeException(extractErrorMessage(body, "Failed to retrieve patient studies"));
        } catch (Exception e) {
            log.error("Failed to fetch studies for patient: patientId={}, error={}", patientId, e.getMessage());
            throw new RuntimeException("Failed to retrieve patient studies: " + e.getMessage(), e);
        }
    }

    public List<Map<String, Object>> getReports(String accessToken) {
        String url = baseUrl + "/reports";
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<ApiResponse<List<Map<String, Object>>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<ApiResponse<List<Map<String, Object>>>>() {}
            );

            if (response.getBody() != null && response.getBody().isSuccess()) {
                return response.getBody().getData();
            }
            throw new RuntimeException("Persisted reports request returned an unsuccessful response");
        } catch (Exception e) {
            log.error("Failed to fetch persisted reports: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch persisted reports", e);
        }
    }

    public Map<String, Object> getReport(String reportId, String accessToken) {
        return getPersistedReport(baseUrl + "/reports/" + reportId, accessToken);
    }

    public Map<String, Object> getReportByStudyId(String studyId, String accessToken) {
        return getPersistedReport(baseUrl + "/reports/study/" + studyId, accessToken);
    }

    public byte[] downloadReportPdf(String reportId, String accessToken) {
        String url = baseUrl + "/reports/" + reportId + "/pdf";
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<byte[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    byte[].class
            );
            if (response.getBody() == null) {
                throw new RuntimeException("Persisted report PDF response was empty");
            }
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to download persisted report PDF: {}", e.getMessage());
            throw new RuntimeException("Failed to download persisted report PDF", e);
        }
    }

    private Map<String, Object> getPersistedReport(String url, String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<ApiResponse<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {}
            );

            if (response.getBody() != null && response.getBody().isSuccess()) {
                return response.getBody().getData();
            }
            return null;
        } catch (org.springframework.web.client.HttpStatusCodeException ex) {
            String body = ex.getResponseBodyAsString();
            log.debug("Persisted report not found at {}", url);
            throw new RuntimeException(extractErrorMessage(body, "Failed to fetch persisted report"));
        } catch (Exception e) {
            log.debug("Persisted report not found at {}: {}", url, e.getMessage());
            throw new RuntimeException("Failed to fetch persisted report: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> triggerWorkflow(String patientId, String studyId, String accessToken) {
        String url = baseUrl + "/integration/workflows/trigger?patientId=" + patientId + "&studyId=" + studyId;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<ApiResponse<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {}
            );

            if (response.getBody() != null && response.getBody().isSuccess()) {
                return response.getBody().getData();
            }
            return null;
        } catch (Exception e) {
            log.error("Failed to trigger workflow: {}", e.getMessage());
            return null;
        }
    }

    public Map<String, Object> submitAiJob(String studyId, String accessToken) {
        return exchangeAiJob(baseUrl + "/ai/jobs", HttpMethod.POST,
                Map.of("studyId", studyId, "taskType", "CBCT_SEGMENTATION"), accessToken);
    }

    public Map<String, Object> getAiJobProgress(String jobId, String accessToken) {
        return exchangeAiJob(baseUrl + "/ai/jobs/" + jobId + "/status", HttpMethod.GET, null, accessToken);
    }

    public Map<String, Object> getAiJob(String jobId, String accessToken) {
        return exchangeAiJob(baseUrl + "/ai/jobs/" + jobId, HttpMethod.GET, null, accessToken);
    }

    public void cancelAiJob(String jobId, String accessToken) {
        exchangeAiJob(baseUrl + "/ai/jobs/" + jobId + "/cancel", HttpMethod.POST, null, accessToken);
    }

    private Map<String, Object> exchangeAiJob(String url, HttpMethod method, Object body, String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<ApiResponse<Map<String, Object>>> response = restTemplate.exchange(url, method,
                    new HttpEntity<>(body, headers), new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {});
            if (response.getBody() != null && response.getBody().isSuccess()) return response.getBody().getData();
            throw new RuntimeException(response.getBody() != null ? response.getBody().getMessage() : "AI service returned an empty response");
        } catch (HttpStatusCodeException ex) {
            throw new RuntimeException(extractErrorMessage(ex.getResponseBodyAsString(), "AI request failed"));
        }
    }

    public Map<String, Object> getWorkflowStatus(String studyId, String accessToken) {
        String url = baseUrl + "/integration/workflows/status/" + studyId;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<ApiResponse<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {}
            );

            if (response.getBody() != null && response.getBody().isSuccess()) {
                return response.getBody().getData();
            }
            return null;
        } catch (Exception e) {
            log.debug("Workflow status check failed for studyId={}", studyId);
            return null;
        }
    }

    public Map<String, Object> initializeUploadSession(String patientId, long totalSize, int totalFiles, String accessToken) {
        String url = baseUrl + "/uploads?patientId=" + patientId + "&totalSize=" + totalSize + "&totalFiles=" + totalFiles;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<ApiResponse<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {}
            );

            if (response.getBody() != null && response.getBody().isSuccess()) {
                return response.getBody().getData();
            }
            return null;
        } catch (Exception e) {
            log.error("Failed to initialize upload session: {}", e.getMessage());
            return null;
        }
    }

    public void uploadFileChunk(String sessionId, String filename, byte[] fileBytes, String accessToken) {
        String url = baseUrl + "/uploads/" + sessionId + "/chunk?fileName=" + filename;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.set("Authorization", "Bearer " + accessToken);
            HttpEntity<byte[]> entity = new HttpEntity<>(fileBytes, headers);

            restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<ApiResponse<Void>>() {}
            );
        } catch (Exception e) {
            log.error("Failed to upload chunk for session: {}, file={}", sessionId, filename, e);
            throw new RuntimeException("Chunk upload failed: " + e.getMessage());
        }
    }

    public Map<String, Object> uploadZipStream(String patientId, byte[] zipBytes, String accessToken) {
        String url = baseUrl + "/uploads/zip?patientId=" + patientId;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.set("Authorization", "Bearer " + accessToken);
            HttpEntity<byte[]> entity = new HttpEntity<>(zipBytes, headers);

            ResponseEntity<ApiResponse<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {}
            );

            if (response.getBody() != null && response.getBody().isSuccess()) {
                return response.getBody().getData();
            }
            return null;
        } catch (Exception e) {
            log.error("Failed to upload ZIP stream to backend: {}", e.getMessage());
            throw new RuntimeException("ZIP upload failed: " + e.getMessage());
        }
    }

    public Map<String, Object> getUploadSessionStatus(String sessionId, String accessToken) {
        String url = baseUrl + "/uploads/" + sessionId;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<ApiResponse<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {}
            );

            if (response.getBody() != null && response.getBody().isSuccess()) {
                return response.getBody().getData();
            }
            return null;
        } catch (Exception e) {
            log.error("Failed to fetch upload session status: {}", e.getMessage());
            return null;
        }
    }

    public UserDto updateProfile(UserDto request, String accessToken) {
        String url = baseUrl + "/auth/profile";
        log.info("Sending update profile request to backend: {}", url);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + accessToken);
            HttpEntity<UserDto> entity = new HttpEntity<>(request, headers);

            ResponseEntity<ApiResponse<UserDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    entity,
                    new ParameterizedTypeReference<ApiResponse<UserDto>>() {}
            );

            if (response.getBody() != null && response.getBody().isSuccess()) {
                return response.getBody().getData();
            } else {
                String msg = response.getBody() != null ? response.getBody().getMessage() : "Unknown backend error";
                throw new RuntimeException(msg);
            }
        } catch (Exception e) {
            log.error("Update profile request failed: {}", e.getMessage());
            throw new RuntimeException("Profile update failed: " + e.getMessage());
        }
    }

    String extractErrorMessage(String body, String defaultMsg) {
        if (body == null || body.isBlank()) {
            return defaultMsg;
        }

        String normalized = body.trim();
        if (normalized.startsWith("{")) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> map = mapper.readValue(body, new TypeReference<Map<String, Object>>() {});
                if (map.containsKey("details") && map.get("details") != null && !map.get("details").toString().isBlank()) {
                    return normalizeErrorMessage(map.get("details").toString(), defaultMsg);
                }
                if (map.containsKey("message") && map.get("message") != null && !map.get("message").toString().isBlank()) {
                    return normalizeErrorMessage(map.get("message").toString(), defaultMsg);
                }
                if (map.containsKey("error") && map.get("error") != null && !map.get("error").toString().isBlank()) {
                    return normalizeErrorMessage(map.get("error").toString(), defaultMsg);
                }
            } catch (Exception ignored) {
                // Fall back to text sanitization
            }
        }

        return normalizeErrorMessage(normalized, defaultMsg);
    }

    private String normalizeErrorMessage(String rawMessage, String defaultMsg) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return defaultMsg;
        }

        String value = rawMessage.replaceAll("\\s+", " ").trim();
        String lower = value.toLowerCase(Locale.ROOT);

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

        return defaultMsg;
    }
}
