package com.canineai.backend.integration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationManager {

    private final RequestDispatcher requestDispatcher;
    private final HealthMonitor healthMonitor;

    /**
     * Executes async task routing mappings directly through integration dispatcher logic.
     */
    public <T, R> Mono<R> routePost(String serviceName, String subPath, T body, Class<R> responseType) {
        log.info("Orchestrating integration dispatch post: Service={}, Subpath={}", serviceName, subPath);
        return requestDispatcher.dispatchPost(serviceName, subPath, body, responseType, new HashMap<>());
    }

    public <R> Mono<R> routeGet(String serviceName, String subPath, Class<R> responseType) {
        log.info("Orchestrating integration dispatch get: Service={}, Subpath={}", serviceName, subPath);
        return requestDispatcher.dispatchGet(serviceName, subPath, responseType, new HashMap<>());
    }

    public void checkIntegrationsHealth() {
        log.info("Running integration services check loops...");
        healthMonitor.monitorAllServices().forEach(status -> 
            log.info("Service: {}, Status: {}, Target: {}", 
                    status.getServiceName(), status.getStatus(), status.getEndpointUrl())
        );
    }
}
