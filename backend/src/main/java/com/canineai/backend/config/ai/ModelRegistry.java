package com.canineai.backend.config.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "canineai.ai")
public class ModelRegistry {

    // Model name to Endpoint coordinates mappings
    private Map<String, ModelEndpoint> models = new HashMap<>();

    @Data
    public static class ModelEndpoint {
        private String name;
        private String version;
        private String url;
        private int timeoutSeconds = 1800;
        private int retryCount = 3;
        private boolean gpuEnabled = true;
        private String fallbackUrl;
    }
}
