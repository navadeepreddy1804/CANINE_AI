package com.canineai.backend.config.ai;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class CloudAIClient {

    private final WebClient webClient;
    private final CloudConfiguration config;

    public CloudAIClient(WebClient.Builder builder, CloudConfiguration config) {
        this.config = config;
        
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, config.getConnectionTimeoutMs())
                .responseTimeout(Duration.ofSeconds(config.getReadTimeoutSeconds()))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(config.getReadTimeoutSeconds(), TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(config.getReadTimeoutSeconds(), TimeUnit.SECONDS)));

        this.webClient = builder
                .baseUrl(config.getServiceUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader("X-Internal-Gateway-Key", config.getGatewayKey())
                .build();
    }

    /**
     * Executes asynchronous post request to GCP GPU microservice endpoints.
     */
    public <T, R> Mono<R> post(String path, T requestBody, Class<R> responseType) {
        log.info("Dispatching secure task payload to Cloud AI GPU infrastructure: {}", path);
        
        return webClient.post()
                .uri(path)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(responseType)
                .retryWhen(Retry.backoff(config.getMaxRetries(), Duration.ofSeconds(config.getBackoffSeconds()))
                        .doBeforeRetry(sig -> log.warn("Retrying Cloud GPU pipeline query. Attempt #{}", sig.totalRetries() + 1)))
                .onErrorResume(err -> {
                    log.error("Fatal: Cloud GPU communication failed. Invoking fallback policies: {}", err.getMessage());
                    return Mono.error(new RuntimeException("Cloud GPU pipeline is offline: " + err.getMessage()));
                });
    }

    /**
     * Executes queries to verify health status configurations.
     */
    public Mono<String> checkHealth() {
        return webClient.get()
                .uri("/api/v1/health")
                .retrieve()
                .bodyToMono(String.class)
                .onErrorReturn("OFFLINE");
    }
}
