package com.canineai.backend.service.report;

import com.canineai.backend.common.BusinessException;
import com.canineai.backend.entity.ClinicalReport;
import com.canineai.backend.entity.ReportStatus;
import com.canineai.backend.entity.ReportStyle;
import com.canineai.backend.repository.ClinicalReportRepository;
import com.canineai.backend.repository.StudyRepository;
import com.canineai.backend.repository.AIJobRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClinicalReportServiceImplTest {

    private static final String DOCTOR_A = "doctor-a@example.com";
    private static final String DOCTOR_B = "doctor-b@example.com";

    @Mock
    private ClinicalReportRepository reportRepository;

    @Mock
    private StudyRepository studyRepository;

    @Mock
    private AIJobRepository jobRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ClinicalReportServiceImpl reportService;

    @Test
    void getReportsForOwner_usesOwnerScopedRepositoryQuery() {
        ClinicalReport reportA = report();
        when(reportRepository.findAllOwned(DOCTOR_A)).thenReturn(List.of(reportA));

        assertThat(reportService.getReportsForOwner(DOCTOR_A))
                .extracting(response -> response.getId())
                .containsExactly(reportA.getId());

        verify(reportRepository).findAllOwned(DOCTOR_A);
    }

    @Test
    void getReportForOwner_doesNotExposeAnotherDoctorsReport() {
        UUID reportId = UUID.randomUUID();
        when(reportRepository.findByIdOwned(reportId, DOCTOR_A)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.getReportForOwner(reportId, DOCTOR_A))
                .isInstanceOf(BusinessException.ResourceNotFoundException.class);

        verify(reportRepository).findByIdOwned(reportId, DOCTOR_A);
    }

    @Test
    void getReportByStudyIdForOwner_usesOwnerScopedRepositoryQuery() {
        ClinicalReport reportA = report();
        when(reportRepository.findByStudyIdOwned(reportA.getStudyId(), DOCTOR_B)).thenReturn(Optional.of(reportA));

        assertThat(reportService.getReportByStudyIdForOwner(reportA.getStudyId(), DOCTOR_B).getId())
                .isEqualTo(reportA.getId());

        verify(reportRepository).findByStudyIdOwned(reportA.getStudyId(), DOCTOR_B);
    }

    private ClinicalReport report() {
        ClinicalReport report = new ClinicalReport();
        report.setId(UUID.randomUUID());
        report.setStudyId(UUID.randomUUID());
        report.setStatus(ReportStatus.COMPLETED);
        report.setReportStyle(ReportStyle.CLINICAL);
        report.setReportMarkdown("Persisted report");
        report.setActiveProvider("persisted");
        report.setPromptTemplateKey("clinical");
        report.setTemplateVersion("1");
        return report;
    }
}
