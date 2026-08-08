package com.canineai.backend.service;

import com.canineai.backend.config.ai.ModelRegistry;
import com.canineai.backend.config.ai.ModelSelector;
import com.canineai.backend.dto.AiJobRequest;
import com.canineai.backend.dto.AiJobResponse;
import com.canineai.backend.entity.AIJob;
import com.canineai.backend.entity.JobState;
import com.canineai.backend.repository.AIJobRepository;
import com.canineai.backend.repository.StudyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiJobServiceImplTest {

    @Mock
    private AIJobRepository jobRepository;

    @Mock
    private StudyRepository studyRepository;

    @Mock
    private ModelSelector modelSelector;

    @Mock
    private InferenceService inferenceService;

    @InjectMocks
    private AiJobServiceImpl aiJobService;

    private UUID studyId;
    private final String doctorEmail = "doctor@example.com";

    @BeforeEach
    void setUp() {
        studyId = UUID.randomUUID();
    }

    @Test
    void submitJob_returnsExistingCompletedJob_withoutTriggeringNewInference() {
        when(studyRepository.existsById(studyId)).thenReturn(true);

        AIJob existingCompletedJob = AIJob.builder()
                .id(UUID.randomUUID())
                .studyId(studyId)
                .taskType(com.canineai.backend.entity.AiTaskType.CBCT_SEGMENTATION)
                .state(JobState.COMPLETED)
                .progressPercentage(100)
                .resultJson("{\"prediction\":{\"prediction\":\"IMPACTED\",\"confidence\":\"95.2%\"}}")
                .build();
        existingCompletedJob.setCreatedAt(LocalDateTime.now().minusHours(1));

        when(jobRepository.findFirstByStudyIdAndDeletedFalseAndStateInOrderByCreatedAtDesc(
                eq(studyId),
                eq(List.of(JobState.QUEUED, JobState.RUNNING, JobState.COMPLETED))
        )).thenReturn(Optional.of(existingCompletedJob));

        AiJobRequest request = new AiJobRequest();
        request.setStudyId(studyId);
        request.setTaskType(com.canineai.backend.entity.AiTaskType.CBCT_SEGMENTATION);
        AiJobResponse response = aiJobService.submitJob(request, doctorEmail);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(existingCompletedJob.getId());
        assertThat(response.getState()).isEqualTo(JobState.COMPLETED);
        assertThat(response.getProgressPercentage()).isEqualTo(100);

        verify(inferenceService, never()).triggerInference(any());
        verify(jobRepository, never()).save(any());
    }

    @Test
    void submitJob_returnsExistingActiveJob_withoutTriggeringNewInference() {
        when(studyRepository.existsById(studyId)).thenReturn(true);

        AIJob existingRunningJob = AIJob.builder()
                .id(UUID.randomUUID())
                .studyId(studyId)
                .taskType(com.canineai.backend.entity.AiTaskType.CBCT_SEGMENTATION)
                .state(JobState.RUNNING)
                .progressPercentage(45)
                .build();

        when(jobRepository.findFirstByStudyIdAndDeletedFalseAndStateInOrderByCreatedAtDesc(
                eq(studyId),
                eq(List.of(JobState.QUEUED, JobState.RUNNING, JobState.COMPLETED))
        )).thenReturn(Optional.of(existingRunningJob));

        AiJobRequest request = new AiJobRequest();
        request.setStudyId(studyId);
        request.setTaskType(com.canineai.backend.entity.AiTaskType.CBCT_SEGMENTATION);
        AiJobResponse response = aiJobService.submitJob(request, doctorEmail);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(existingRunningJob.getId());
        assertThat(response.getState()).isEqualTo(JobState.RUNNING);

        verify(inferenceService, never()).triggerInference(any());
        verify(jobRepository, never()).save(any());
    }

    @Test
    void submitJob_createsNewJobAndTriggersInference_whenNoExistingJob() {
        when(studyRepository.existsById(studyId)).thenReturn(true);
        com.canineai.backend.entity.Study dummyStudy = new com.canineai.backend.entity.Study();
        dummyStudy.setId(studyId);
        when(studyRepository.findById(studyId)).thenReturn(Optional.of(dummyStudy));
        when(jobRepository.findFirstByStudyIdAndDeletedFalseAndStateInOrderByCreatedAtDesc(
                eq(studyId),
                eq(List.of(JobState.QUEUED, JobState.RUNNING, JobState.COMPLETED))
        )).thenReturn(Optional.empty());

        ModelRegistry.ModelEndpoint endpoint = new ModelRegistry.ModelEndpoint();
        endpoint.setName("ToothSeg");
        endpoint.setVersion("v2.1.0");
        when(modelSelector.selectModel(com.canineai.backend.entity.AiTaskType.CBCT_SEGMENTATION)).thenReturn(endpoint);

        UUID newJobId = UUID.randomUUID();
        when(jobRepository.save(any(AIJob.class))).thenAnswer(inv -> {
            AIJob job = inv.getArgument(0);
            job.setId(newJobId);
            return job;
        });

        AiJobRequest request = new AiJobRequest();
        request.setStudyId(studyId);
        request.setTaskType(com.canineai.backend.entity.AiTaskType.CBCT_SEGMENTATION);
        AiJobResponse response = aiJobService.submitJob(request, doctorEmail);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(newJobId);
        assertThat(response.getState()).isEqualTo(JobState.QUEUED);
        assertThat(response.getProgressPercentage()).isEqualTo(0);

        verify(jobRepository).save(any(AIJob.class));
        verify(inferenceService).triggerInference(newJobId);
    }
}
