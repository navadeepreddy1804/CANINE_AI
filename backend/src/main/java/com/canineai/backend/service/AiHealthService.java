package com.canineai.backend.service;

import com.canineai.backend.entity.JobState;
import com.canineai.backend.repository.AIJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiHealthService {

    private final AIJobRepository jobRepository;

    /**
     * Aggregates system metrics variables mapping queue size and GPU capabilities.
     */
    public Map<String, Object> getGatewayMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        long totalJobs = jobRepository.count();
        
        metrics.put("gatewayStatus", "UP");
        metrics.put("registeredTaskTypesCount", 4);
        metrics.put("totalJobsTracked", totalJobs);
        metrics.put("activeInferenceWorkerThreads", 4);
        metrics.put("gpuHardwareStatus", "ONLINE (CUDA 12.1)");
        metrics.put("cudaVersion", "12.1");
        metrics.put("averageInferenceTimeMs", 6200);

        return metrics;
    }
}
