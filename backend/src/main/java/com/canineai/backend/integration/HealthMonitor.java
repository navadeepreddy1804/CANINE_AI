package com.canineai.backend.integration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HealthMonitor {

    private final IntegrationRegistry registry;
    private final IntegrationMetrics metrics;

    /**
     * Aggregates active status parameters for monitored routes.
     */
    public List<ServiceStatus> monitorAllServices() {
        List<ServiceStatus> statuses = new ArrayList<>();
        
        registry.getRegisteredEndpoints().forEach((name, url) -> {
            boolean hasErrors = metrics.getErrorCount(name) > 0;
            statuses.add(ServiceStatus.builder()
                    .serviceName(name)
                    .status(hasErrors ? "DEGRADED" : "ONLINE")
                    .endpointUrl(url)
                    .latencyMs(0.0) // Latency tracking not implemented
                    .lastCheckTime(LocalDateTime.now())
                    .build());
        });
        
        return statuses;
    }
}
