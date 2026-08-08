package com.canineai.backend.integration;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CircuitBreakerManager {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    /**
     * Resolves the resilience4j circuit breaker instance for the target integration.
     */
    public CircuitBreaker getCircuitBreaker(String serviceName) {
        log.info("Resolving circuit breaker mapping for: {}", serviceName);
        return circuitBreakerRegistry.circuitBreaker(serviceName);
    }
}
