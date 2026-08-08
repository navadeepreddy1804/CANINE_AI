package com.canineai.backend.config.ai;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
public class AiGatewayConfig {

    @Value("${canineai.ai.gateway.api-key:canineaiInternalGatewayApiKeyPayload}")
    private String gatewayApiKey;

    @Bean
    public WebClient aiWebClient(WebClient.Builder builder) {
        log.info("Initializing non-blocking Spring WebClient pipeline for FastAPI communication...");

        // Configure custom thread connection pooling to ensure high-throughput non-blocking operations
        ConnectionProvider provider = ConnectionProvider.builder("ai-gateway-pool")
                .maxConnections(50)
                .pendingAcquireTimeout(Duration.ofSeconds(15))
                .maxIdleTime(Duration.ofSeconds(20))
                .build();

        HttpClient httpClient = HttpClient.create(provider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000) // 10s connect timeout
                .responseTimeout(Duration.ofSeconds(60)) // 60s global response timeout
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(60, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(60, TimeUnit.SECONDS)));

        return builder
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                // Sign requests with security API Keys
                .defaultHeader("X-Internal-Gateway-Key", gatewayApiKey)
                .filter((request, next) -> {
                    String correlationId = org.slf4j.MDC.get("correlationId");
                    if (correlationId == null) {
                        correlationId = java.util.UUID.randomUUID().toString();
                    }
                    org.springframework.web.reactive.function.client.ClientRequest filtered = 
                            org.springframework.web.reactive.function.client.ClientRequest.from(request)
                                    .header("X-Correlation-ID", correlationId)
                                    .build();
                    log.info("Outgoing AI Request: {} {}", filtered.method(), filtered.url());
                    return next.exchange(filtered).doOnNext(response -> {
                        log.info("Completed AI Request: {} {} - Status: {}", filtered.method(), filtered.url(), response.statusCode());
                    });
                })
                .build();
    }
}
