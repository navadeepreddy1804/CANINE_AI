package com.canineai.backend.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.annotation.PostConstruct;

@Slf4j
@Component
public class IntegrationRegistry {

    @Value("${canineai.ai.gateway.primary-url:http://localhost:8000}")
    private String defaultAiUrl;

    @Value("${canineai.llm.api-url:https://generativelanguage.googleapis.com}")
    private String defaultLlmUrl;

    private final Map<String, String> endpointMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // Register default configurations
        registerEndpoint("ai-service", defaultAiUrl);
        registerEndpoint("llm-service", defaultLlmUrl);
        registerEndpoint("storage-service", "local://uploads");
        registerEndpoint("notification-service", "internal://event-bus");
        log.info("Initialized CanineAI Enterprise Integration Registry with default routes.");
    }

    public void registerEndpoint(String name, String url) {
        if (name == null || url == null) return;
        endpointMap.put(name.toLowerCase(), url);
        log.info("Registered integration route: {} -> {}", name, url);
    }

    public String getEndpoint(String name) {
        return endpointMap.get(name.toLowerCase());
    }

    public Map<String, String> getRegisteredEndpoints() {
        return endpointMap;
    }
}
