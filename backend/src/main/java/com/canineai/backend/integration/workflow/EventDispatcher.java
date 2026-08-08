package com.canineai.backend.integration.workflow;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventDispatcher {

    private final ApplicationEventPublisher eventPublisher;

    /**
     * Publishes workflow events asynchronously across Spring transaction cycles.
     */
    public void dispatchEvent(Object event) {
        log.info("Dispatching workflow event: {}", event.getClass().getSimpleName());
        eventPublisher.publishEvent(event);
    }
}
