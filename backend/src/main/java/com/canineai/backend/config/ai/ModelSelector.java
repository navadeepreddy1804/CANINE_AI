package com.canineai.backend.config.ai;

import com.canineai.backend.common.BusinessException;
import com.canineai.backend.entity.AiTaskType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModelSelector {

    private final ModelRegistry modelRegistry;

    /**
     * Resolves the configured model configuration matching a task type.
     */
    public ModelRegistry.ModelEndpoint selectModel(AiTaskType taskType) {
        String key = resolveKey(taskType);
        ModelRegistry.ModelEndpoint endpoint = modelRegistry.getModels().get(key);
        
        if (endpoint == null) {
            log.warn("No configured AI Model found in properties registry for task: {}. Loading default mock configurations.", taskType);
            // Default mock fallback configurations
            endpoint = new ModelRegistry.ModelEndpoint();
            endpoint.setName("Dataset121_ToothFairy2_Teeth");
            endpoint.setVersion("1.0.0");
            endpoint.setUrl("http://localhost:8002/api/v1/inference");
            endpoint.setFallbackUrl("http://localhost:8002/api/v1/inference");
            endpoint.setTimeoutSeconds(1800);
        }

        return endpoint;
    }

    private String resolveKey(AiTaskType taskType) {
        return switch (taskType) {
            case CBCT_SEGMENTATION -> "segmentation";
            case METADATA_EXTRACTION -> "metadata";
            case CANINE_LOCALIZATION -> "localization";
            case CLINICAL_MEASUREMENTS -> "measurements";
        };
    }
}
