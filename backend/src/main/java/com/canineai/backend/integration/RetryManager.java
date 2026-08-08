package com.canineai.backend.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.util.retry.Retry;
import java.time.Duration;

@Slf4j
@Component
public class RetryManager {

    /**
     * Generates a reactive backoff retry policy strategy configuration.
     */
    public Retry getRetrySpec(String serviceName, int maxAttempts, int backoffSeconds) {
        return Retry.backoff(maxAttempts, Duration.ofSeconds(backoffSeconds))
                .doBeforeRetry(retrySignal -> log.warn("Retrying failed API integration request to '{}'. Attempt #{}", 
                        serviceName, retrySignal.totalRetries() + 1));
    }
}
