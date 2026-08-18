package com.canineai.backend.service;

import com.canineai.backend.config.ai.ModelRegistry;
import com.canineai.backend.config.ai.ModelSelector;
import com.canineai.backend.entity.AIJob;
import com.canineai.backend.entity.JobState;
import com.canineai.backend.entity.Study;
import com.canineai.backend.entity.StudyStatus;
import com.canineai.backend.repository.AIJobRepository;
import com.canineai.backend.repository.StudyRepository;
import com.canineai.backend.repository.StudyStorageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RealInferenceServiceImplTest {

    @Mock private AIJobRepository jobRepository;
    @Mock private StudyRepository studyRepository;
    @Mock private StudyStorageRepository studyStorageRepository;
    @Mock private InferenceHelper inferenceHelper;
    @Mock private ModelSelector modelSelector;
    @Mock private WebClient aiWebClient;

    @InjectMocks private RealInferenceServiceImpl inferenceService;

    @Test
    void triggerInference_updatesJobStateToQueued() {
        UUID jobId = UUID.randomUUID();
        UUID studyId = UUID.randomUUID();
        AIJob job = AIJob.builder()
                .id(jobId)
                .studyId(studyId)
                .state(JobState.CLAIMED)
                .taskType(com.canineai.backend.entity.AiTaskType.CBCT_SEGMENTATION)
                .build();

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        inferenceService.triggerInference(jobId);

        verify(jobRepository).save(job);
        assertThat(job.getState()).isEqualTo(JobState.QUEUED);
        assertThat(job.getProgressPercentage()).isEqualTo(10);
        assertThat(job.getCurrentStage()).isEqualTo("QUEUED");
    }
}
