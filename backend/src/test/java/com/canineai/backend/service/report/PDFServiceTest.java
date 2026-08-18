package com.canineai.backend.service.report;

import com.canineai.backend.common.BusinessException;
import com.canineai.backend.dto.ReportResponse;
import com.canineai.backend.entity.AIJob;
import com.canineai.backend.entity.Gender;
import com.canineai.backend.entity.Patient;
import com.canineai.backend.entity.Study;
import com.canineai.backend.repository.AIJobRepository;
import com.canineai.backend.repository.StudyRepository;
import com.canineai.backend.repository.UploadedFileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PDFServiceTest {

    private static final String DOCTOR_A = "doctor-a@example.com";
    private static final String DOCTOR_B = "doctor-b@example.com";

    @Mock
    private ClinicalReportService clinicalReportService;

    @Mock
    private StudyRepository studyRepository;

    @Mock
    private AIJobRepository aiJobRepository;

    @Mock
    private UploadedFileRepository uploadedFileRepository;

    @Mock
    private com.canineai.backend.repository.StudyStorageRepository studyStorageRepository;

    @Mock
    private PDFExporter pdfExporter;

    @InjectMocks
    private PDFServiceImpl pdfService;

    @Test
    void renderPersistedPdf_rendersOnlyStoredReportStudyPatientAndAiJobData() {
        ReportResponse report = report();
        Study study = study();
        AIJob job = new AIJob();
        job.setEndTime(LocalDateTime.of(2026, 7, 27, 10, 30));
        job.setResultJson("{\"prediction\":\"Impacted\",\"confidence\":0.91}");
        when(clinicalReportService.getReportForOwner(report.getId(), DOCTOR_A)).thenReturn(report);
        when(studyRepository.findById(report.getStudyId())).thenReturn(Optional.of(study));
        when(aiJobRepository.findFirstByStudyIdAndDeletedFalseOrderByEndTimeDesc(study.getId()))
                .thenReturn(Optional.of(job));
        when(pdfExporter.exportPdf(any(), eq(List.of()))).thenReturn(new byte[] {1, 2, 3});

        byte[] pdf = pdfService.renderPersistedPdf(report.getId(), DOCTOR_A);

        ArgumentCaptor<String> content = ArgumentCaptor.forClass(String.class);
        verify(pdfExporter).exportPdf(content.capture(), eq(List.of()));
        assertThat(pdf).containsExactly(1, 2, 3);
        assertThat(content.getValue())
                .contains("Stored report content", "Jane Doe", "PT-00001", "CT");
        verify(clinicalReportService).getReportForOwner(report.getId(), DOCTOR_A);
        verify(uploadedFileRepository, never()).findBySessionId(any());
    }

    @Test
    void renderPersistedPdf_rejectsAnotherDoctorsReportBeforeReadingPersistedData() {
        UUID doctorAReportId = UUID.randomUUID();
        when(clinicalReportService.getReportForOwner(doctorAReportId, DOCTOR_B))
                .thenThrow(new BusinessException.ResourceNotFoundException("Clinical Report not found"));

        assertThatThrownBy(() -> pdfService.renderPersistedPdf(doctorAReportId, DOCTOR_B))
                .isInstanceOf(BusinessException.ResourceNotFoundException.class);

        verify(clinicalReportService).getReportForOwner(doctorAReportId, DOCTOR_B);
        verify(studyRepository, never()).findById(any());
        verify(pdfExporter, never()).exportPdf(any(), any());
    }

    private ReportResponse report() {
        return ReportResponse.builder()
                .id(UUID.randomUUID())
                .studyId(UUID.randomUUID())
                .reportMarkdown("Stored report content")
                .build();
    }

    private Study study() {
        Patient patient = new Patient();
        patient.setFullName("Jane Doe");
        patient.setHospitalPatientId("PT-00001");
        patient.setDateOfBirth(LocalDate.of(2000, 1, 1));
        patient.setGender(Gender.FEMALE);

        Study study = new Study();
        study.setId(UUID.randomUUID());
        study.setPatient(patient);
        study.setModality("CT");
        study.setRows(512);
        study.setColumns(512);
        study.setVoxelSize("0.2 mm");
        return study;
    }
}
