package com.canineai.backend.controller;

import com.canineai.backend.common.BusinessException;
import com.canineai.backend.dto.ReportResponse;
import com.canineai.backend.service.report.ClinicalReportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClinicalReportControllerTest {

    private static final String DOCTOR_A = "doctor-a@example.com";
    private static final String DOCTOR_B = "doctor-b@example.com";

    @Mock
    private ClinicalReportService reportService;

    @InjectMocks
    private ClinicalReportController controller;

    @Test
    void getReports_returnsOnlyAuthenticatedDoctorsPersistedReports() {
        ReportResponse doctorAReport = ReportResponse.builder().id(UUID.randomUUID()).build();
        when(reportService.getReportsForOwner(DOCTOR_A)).thenReturn(List.of(doctorAReport));

        var response = controller.getReports(principal(DOCTOR_A));

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).containsExactly(doctorAReport);
        verify(reportService).getReportsForOwner(DOCTOR_A);
    }

    @Test
    void getReport_usesAuthenticatedDoctorsOwnerScopedServiceMethod() {
        UUID reportId = UUID.randomUUID();
        ReportResponse report = ReportResponse.builder().id(reportId).build();
        when(reportService.getReportForOwner(reportId, DOCTOR_A)).thenReturn(report);

        var response = controller.getReport(reportId, principal(DOCTOR_A));

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isEqualTo(report);
        verify(reportService).getReportForOwner(reportId, DOCTOR_A);
    }

    @Test
    void getReport_doesNotExposeDoctorAsReportToDoctorB() {
        UUID doctorAReportId = UUID.randomUUID();
        when(reportService.getReportForOwner(doctorAReportId, DOCTOR_B))
                .thenThrow(new BusinessException.ResourceNotFoundException("Clinical Report not found"));

        assertThatThrownBy(() -> controller.getReport(doctorAReportId, principal(DOCTOR_B)))
                .isInstanceOf(BusinessException.ResourceNotFoundException.class);

        verify(reportService).getReportForOwner(doctorAReportId, DOCTOR_B);
    }

    @Test
    void getReportByStudyId_usesAuthenticatedDoctorsOwnerScopedServiceMethod() {
        UUID studyId = UUID.randomUUID();
        ReportResponse report = ReportResponse.builder().id(UUID.randomUUID()).studyId(studyId).build();
        when(reportService.getReportByStudyIdForOwner(studyId, DOCTOR_A)).thenReturn(report);

        var response = controller.getReportByStudyId(studyId, principal(DOCTOR_A));

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isEqualTo(report);
        verify(reportService).getReportByStudyIdForOwner(studyId, DOCTOR_A);
    }

    @Test
    void getReportByStudyId_doesNotExposeDoctorAsStudyReportToDoctorB() {
        UUID doctorAStudyId = UUID.randomUUID();
        when(reportService.getReportByStudyIdForOwner(doctorAStudyId, DOCTOR_B))
                .thenThrow(new BusinessException.ResourceNotFoundException("Clinical Report not found for study"));

        assertThatThrownBy(() -> controller.getReportByStudyId(doctorAStudyId, principal(DOCTOR_B)))
                .isInstanceOf(BusinessException.ResourceNotFoundException.class);

        verify(reportService).getReportByStudyIdForOwner(doctorAStudyId, DOCTOR_B);
    }

    @Test
    void reportReadsRejectAnonymousRequestsBeforeCallingTheService() {
        assertThatThrownBy(() -> controller.getReports(null))
                .isInstanceOf(BusinessException.UnauthorizedException.class);
        assertThatThrownBy(() -> controller.getReport(UUID.randomUUID(), null))
                .isInstanceOf(BusinessException.UnauthorizedException.class);
        assertThatThrownBy(() -> controller.getReportByStudyId(UUID.randomUUID(), null))
                .isInstanceOf(BusinessException.UnauthorizedException.class);

        verifyNoInteractions(reportService);
    }

    private Principal principal(String doctor) {
        return () -> doctor;
    }
}
