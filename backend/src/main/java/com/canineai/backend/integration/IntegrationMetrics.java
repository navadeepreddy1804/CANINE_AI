package com.canineai.backend.integration;

import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class IntegrationMetrics {

    private final Map<String, AtomicLong> dispatchCount = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> errorCount = new ConcurrentHashMap<>();

    public void incrementDispatch(String serviceName) {
        dispatchCount.computeIfAbsent(serviceName.toLowerCase(), k -> new AtomicLong(0)).incrementAndGet();
    }

    public void incrementError(String serviceName) {
        errorCount.computeIfAbsent(serviceName.toLowerCase(), k -> new AtomicLong(0)).incrementAndGet();
    }

    public long getDispatchCount(String serviceName) {
        AtomicLong val = dispatchCount.get(serviceName.toLowerCase());
        return val != null ? val.get() : 0;
    }

    public long getErrorCount(String serviceName) {
        AtomicLong val = errorCount.get(serviceName.toLowerCase());
        return val != null ? val.get() : 0;
    }
}
