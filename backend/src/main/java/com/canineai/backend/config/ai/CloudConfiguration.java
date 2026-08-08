package com.canineai.backend.config.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "canineai.cloud-ai")
public class CloudConfiguration {

    private String serviceUrl = "http://localhost:8000"; // fallback GCP VM active IP url
    private String gatewayKey = "canineaiInternalGatewayApiKeyPayload";
    private int connectionTimeoutMs = 15000;
    private int readTimeoutSeconds = 90;
    private int maxRetries = 3;
    private int backoffSeconds = 2;
}
