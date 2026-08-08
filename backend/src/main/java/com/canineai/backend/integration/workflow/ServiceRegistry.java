package com.canineai.backend.integration.workflow;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ServiceRegistry {

    private final Map<String, String> services = new ConcurrentHashMap<>();

    public void registerService(String serviceId, String url) {
        services.put(serviceId.toLowerCase(), url);
        log.info("Registered microservice endpoint: {} -> {}", serviceId, url);
    }

    public String getServiceUrl(String serviceId) {
        return services.get(serviceId.toLowerCase());
    }

    public void deregisterService(String serviceId) {
        services.remove(serviceId.toLowerCase());
        log.info("Deregistered microservice endpoint: {}", serviceId);
    }
}
