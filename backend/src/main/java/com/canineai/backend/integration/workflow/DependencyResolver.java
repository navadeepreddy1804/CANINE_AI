package com.canineai.backend.integration.workflow;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DependencyResolver {

    private final ApplicationContext applicationContext;

    /**
     * Resolves beans dynamically by type class context.
     */
    public <T> T resolve(Class<T> beanClass) {
        return applicationContext.getBean(beanClass);
    }

    /**
     * Resolves beans dynamically by name context.
     */
    public Object resolveByName(String name) {
        return applicationContext.getBean(name);
    }
}
