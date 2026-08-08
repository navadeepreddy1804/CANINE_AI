package com.canineai.backend.integration.workflow;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;

@Slf4j
@Configuration
@EnableAsync
public class IntegrationConfiguration {

    @Bean(name = "workflowAsyncExecutor")
    public Executor workflowAsyncExecutor() {
        log.info("Initializing workflow thread pool task executor pool...");
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("CanineAI-Workflow-");
        executor.initialize();
        return executor;
    }
}
