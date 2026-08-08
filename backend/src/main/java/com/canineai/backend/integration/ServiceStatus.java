package com.canineai.backend.integration;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ServiceStatus {
    private String serviceName;
    private String status; // ONLINE, DEGRADED, OFFLINE
    private String endpointUrl;
    private double latencyMs;
    private LocalDateTime lastCheckTime;
}
