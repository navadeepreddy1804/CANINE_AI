package com.canineai.backend.integration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceDiscovery {

    private final IntegrationRegistry registry;

    /**
     * Resolves target service URL details.
     */
    public String discoverService(String serviceName) {
        String endpoint = registry.getEndpoint(serviceName);
        if (endpoint == null) {
            log.error("Failed to discover active integration routing for service: {}", serviceName);
            throw new IllegalArgumentException("Unknown integration service target: " + serviceName);
        }
        return endpoint;
    }
}
