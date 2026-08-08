package com.canineai.backend.config.async;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

/**
 * Async executor configuration.
 *
 * Provides a dedicated thread pool for @Async methods (welcome emails, background
 * notifications, etc.) and installs an uncaught-exception handler so that any
 * exception escaping an @Async method is logged rather than silently swallowed
 * or — worse — propagated back to the calling thread.
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Override
    @Bean(name = "taskExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("CanineAI-Async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        log.info("CanineAI async task executor initialised (core={}, max={}, queue={})",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), 50);
        return executor;
    }

    /**
     * Any uncaught exception from an @Async method is caught here and logged.
     * It is NEVER re-thrown so it cannot affect callers or transactions.
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (Throwable ex, Method method, Object... params) ->
                log.error("Uncaught exception in @Async method {}.{}(): {}",
                        method.getDeclaringClass().getSimpleName(),
                        method.getName(),
                        ex.getMessage(), ex);
    }
}
