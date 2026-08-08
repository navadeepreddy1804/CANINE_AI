package com.canineai.backend.controller;

import com.canineai.backend.common.BusinessException;
import com.canineai.backend.service.report.PDFService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.Principal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PDFReportControllerTest {

    private static final String DOCTOR_A = "doctor-a@example.com";
    private static final String DOCTOR_B = "doctor-b@example.com";

    @Mock
    private PDFService pdfService;

    @InjectMocks
    private PDFReportController controller;

    @Test
    void downloadPersistedPdf_allowsDoctorAToDownloadTheirReport() {
        UUID reportId = UUID.randomUUID();
        byte[] pdf = {1, 2, 3};
        when(pdfService.renderPersistedPdf(reportId, DOCTOR_A)).thenReturn(pdf);

        var response = controller.downloadPersistedPdf(reportId, principal(DOCTOR_A));

        assertThat(response.getBody()).isEqualTo(pdf);
        verify(pdfService).renderPersistedPdf(reportId, DOCTOR_A);
    }

    @Test
    void downloadPersistedPdf_doesNotExposeDoctorAsReportToDoctorB() {
        UUID doctorAReportId = UUID.randomUUID();
        when(pdfService.renderPersistedPdf(doctorAReportId, DOCTOR_B))
                .thenThrow(new BusinessException.ResourceNotFoundException("Clinical Report not found"));

        assertThatThrownBy(() -> controller.downloadPersistedPdf(doctorAReportId, principal(DOCTOR_B)))
                .isInstanceOf(BusinessException.ResourceNotFoundException.class);

        verify(pdfService).renderPersistedPdf(doctorAReportId, DOCTOR_B);
    }

    @Test
    void downloadPersistedPdf_rejectsAnonymousAccessBeforeCallingService() {
        assertThatThrownBy(() -> controller.downloadPersistedPdf(UUID.randomUUID(), null))
                .isInstanceOf(BusinessException.UnauthorizedException.class);

        verifyNoInteractions(pdfService);
    }

    private Principal principal(String doctor) {
        return () -> doctor;
    }
}
