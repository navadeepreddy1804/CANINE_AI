package com.canineai.backend.service;

import com.canineai.backend.dto.UploadSessionResponse;
import com.canineai.backend.entity.Study;
import com.canineai.backend.entity.StudyStatus;
import com.canineai.backend.entity.UploadSession;
import com.canineai.backend.repository.PatientRepository;
import com.canineai.backend.repository.SeriesRepository;
import com.canineai.backend.repository.StudyRepository;
import com.canineai.backend.repository.StudyStorageRepository;
import com.canineai.backend.repository.UploadSessionRepository;
import com.canineai.backend.repository.UploadedFileRepository;
import com.canineai.backend.repository.UserRepository;
import com.canineai.backend.storage.StorageProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UploadServiceImplTest {

    @Mock private UploadSessionRepository sessionRepository;
    @Mock private UploadedFileRepository uploadedFileRepository;
    @Mock private StudyRepository studyRepository;
    @Mock private SeriesRepository seriesRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private UserRepository userRepository;
    @Mock private StudyStorageRepository studyStorageRepository;
    @Mock private StorageProvider storageProvider;
    @Mock private DicomValidator dicomValidator;
    @Mock private MetadataExtractor metadataExtractor;
    @Mock private WebClient aiWebClient;

    @InjectMocks private UploadServiceImpl uploadService;

    @Test
    void completedUploadResponseIncludesThePersistedStudyId() {
        UUID sessionId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID studyId = UUID.randomUUID();
        UploadSession session = UploadSession.builder()
                .id(sessionId)
                .patientId(patientId)
                .username("doctor@example.com")
                .totalSizeBytes(1024L)
                .totalFilesCount(1)
                .uploadedFilesCount(1)
                .status(StudyStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .build();
        Study persistedStudy = Study.builder().id(studyId).uploadSessionId(sessionId).build();

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(studyRepository.findByUploadSessionIdAndDeletedFalse(sessionId))
                .thenReturn(Optional.of(persistedStudy));

        UploadSessionResponse response = uploadService.getSession(sessionId);

        assertThat(response.getId()).isEqualTo(sessionId);
        assertThat(response.getUploadSessionId()).isEqualTo(sessionId);
        assertThat(response.getPatientId()).isEqualTo(patientId);
        assertThat(response.getStudyId()).isEqualTo(studyId);
        assertThat(response.getStudyId()).isEqualTo(persistedStudy.getId());
    }

    @Test
    void processZipStream_acceptsUnextendedDicomFiles_andSetsCorrectStudyOwner() throws Exception {
        UUID sessionId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        String doctorEmail = "doctor@example.com";

        UploadSession session = UploadSession.builder()
                .id(sessionId)
                .patientId(patientId)
                .username(doctorEmail)
                .status(StudyStatus.UPLOADING)
                .build();

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        com.canineai.backend.entity.UploadedFile mockFile = com.canineai.backend.entity.UploadedFile.builder()
                .id(UUID.randomUUID())
                .fileName("IM-0001-0001")
                .storageLocationPath("temp/" + sessionId + "/IM-0001-0001")
                .fileSizeBytes(1024L)
                .build();
        when(uploadedFileRepository.findBySessionId(sessionId)).thenReturn(java.util.List.of(mockFile));

        com.canineai.backend.entity.Patient mockPatient = com.canineai.backend.entity.Patient.builder()
                .id(patientId)
                .fullName("Test Patient")
                .hospitalPatientId("HP-100")
                .build();
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(mockPatient));
        when(dicomValidator.isValidDicom(org.mockito.ArgumentMatchers.any())).thenReturn(true);

        MetadataExtractor.DicomMetadata mockMeta = MetadataExtractor.DicomMetadata.builder()
                .studyInstanceUid("UID-12345")
                .seriesInstanceUid("SER-12345")
                .sopInstanceUid("SOP-12345")
                .studyDate(java.time.LocalDate.now())
                .studyTime("120000")
                .modality("CT")
                .studyDescription("Test CBCT")
                .manufacturer("Test")
                .deviceModel("Test")
                .sliceThickness(0.3)
                .voxelSize("0.3")
                .pixelSpacing("0.3")
                .rows(512)
                .columns(512)
                .sliceCount(1)
                .seriesDescription("Test Series")
                .build();
        when(metadataExtractor.extract(org.mockito.ArgumentMatchers.any())).thenReturn(mockMeta);

        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        org.mockito.ArgumentCaptor<Study> studyCaptor = org.mockito.ArgumentCaptor.forClass(Study.class);
        org.mockito.Mockito.doAnswer(inv -> {
            Study s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            latch.countDown();
            return s;
        }).when(studyRepository).save(studyCaptor.capture());

        // Create a memory ZIP stream with a file without dot extension (common in DICOM studies)
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(baos)) {
            zos.putNextEntry(new java.util.zip.ZipEntry("IM-0001-0001"));
            zos.write("DUMMY DICOM CONTENT".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(baos.toByteArray());

        uploadService.processZipStream(sessionId, bais);

        // Verify that the unextended slice file was NOT skipped during ZIP unpacking
        org.mockito.ArgumentCaptor<com.canineai.backend.entity.UploadedFile> fileCaptor = org.mockito.ArgumentCaptor.forClass(com.canineai.backend.entity.UploadedFile.class);
        org.mockito.Mockito.verify(uploadedFileRepository, org.mockito.Mockito.timeout(2000).atLeastOnce()).save(fileCaptor.capture());
        assertThat(fileCaptor.getValue().getFileName()).isEqualTo("IM-0001-0001");

        boolean completed = latch.await(4, java.util.concurrent.TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        // Crucial check: Study createdBy MUST match session username ("doctor@example.com"), NOT "System"
        assertThat(studyCaptor.getValue().getCreatedBy()).isEqualTo(doctorEmail);
    }
}

