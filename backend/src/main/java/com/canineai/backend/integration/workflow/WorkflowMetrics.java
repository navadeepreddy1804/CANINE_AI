package com.canineai.backend.integration.workflow;

import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class WorkflowMetrics {

    private final Map<String, AtomicLong> stateHits = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> stateErrors = new ConcurrentHashMap<>();

    public void incrementStateHit(WorkflowState state) {
        stateHits.computeIfAbsent(state.name(), k -> new AtomicLong(0)).incrementAndGet();
    }

    public void incrementStateError(WorkflowState state) {
        stateErrors.computeIfAbsent(state.name(), k -> new AtomicLong(0)).incrementAndGet();
    }

    public long getStateHit(WorkflowState state) {
        AtomicLong val = stateHits.get(state.name());
        return val != null ? val.get() : 0;
    }

    public long getStateError(WorkflowState state) {
        AtomicLong val = stateErrors.get(state.name());
        return val != null ? val.get() : 0;
    }
}
