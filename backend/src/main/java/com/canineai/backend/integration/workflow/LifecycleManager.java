package com.canineai.backend.integration.workflow;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LifecycleManager implements SmartLifecycle {

    private boolean running = false;

    @Override
    public void start() {
        log.info("CanineAI Enterprise Integration Layer starting up...");
        // Setup initial integration connections
        running = true;
        log.info("CanineAI Enterprise Integration Layer successfully initialized.");
    }

    @Override
    public void stop() {
        log.info("CanineAI Enterprise Integration Layer shutting down gracefully...");
        running = false;
        log.info("CanineAI Enterprise Integration Layer stopped.");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        // Run early in lifecycle phase order sequence
        return Integer.MAX_VALUE - 10;
    }
}
